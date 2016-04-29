package org.rapla.rest.client.gwt.internal.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.xhr.client.XMLHttpRequest;
import org.rapla.rest.client.AsyncCallback;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.ExceptionDeserializer;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.SerializableExceptionInformation.SerializableExceptionStacktraceInformation;
import org.rapla.rest.client.gwt.internal.RemoteJsonException;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GwtClientServerConnector<T>
{
    protected static final JavaScriptObject jsonParser;
    private static AsyncCallback _callback;
    static
    {
        jsonParser = getJsonParser();
    }

    public static void registerSingleThreadedCallback(AsyncCallback callback)
    {
        _callback = callback;
    }

    private String httpMethod;

    private static native JavaScriptObject getJsonParser()
    /*-{
		return $wnd.JSON.parse;
    }-*/;

    protected final ExceptionDeserializer exceptionDeserializer;
    protected final String url;
    protected final String body;
    protected final ResultDeserializer<T> resultDeserializer;

    private String token;
    private final Map<String, String> additionalHeaders;
    private final String resultType;

    public GwtClientServerConnector(String httpMethod, final ExceptionDeserializer exceptionDeserializer, final String url, Map<String, String> additionalHeaders,
            final String requestParams, final ResultDeserializer<T> resultDeserializer, String resultType)
    {
        this.httpMethod = httpMethod;
        this.exceptionDeserializer = exceptionDeserializer;
        this.url = url;
        this.additionalHeaders = additionalHeaders;
        this.body = requestParams;
        this.resultDeserializer = resultDeserializer;
        this.resultType = resultType;
    }

    public static <T> T doInvoke(final String requestMethodType, final String url, final String body, final Map<String, String> additionalHeaders,
            final ResultDeserializer<T> ser, String resultType, CustomConnector connector) throws Exception
    {
        GwtClientServerConnector gwtConnector = new GwtClientServerConnector<>(requestMethodType, connector, url, additionalHeaders, body, ser, resultType);
        final String accessToken = connector.getAccessToken();
        if (accessToken != null)
        {
            gwtConnector.setToken(accessToken);
        }
        if (_callback != null)
        {
            try
            {
                gwtConnector.send(_callback);
            }
            finally
            {
                _callback = null;
            }
            return null;
        }
        else
        {
            return (T)gwtConnector.send(null);
        }
    }

    protected Map<String, String> getAdditionalHeaders()
    {
        return additionalHeaders;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public static class SynchronousXHR extends XMLHttpRequest
    {

        protected SynchronousXHR()
        {
        }

        public native final void synchronousOpen(String method, String uri)
        /*-{
            this.open(method, uri, false);
        }-*/;

        public native final void setTimeout(int timeoutP)
        /*-{
            this.timeout = timeoutP;
        }-*/;
    }

    public T send(AsyncCallback callback) throws Exception
    {
        XMLHttpRequest request = callback != null ? XMLHttpRequest.create():SynchronousXHR.create();
        if (callback != null)
        {
            request.open(httpMethod, url);
        }
        else
        {
            ((SynchronousXHR) request).synchronousOpen(httpMethod, url);
        }
        // request.open(httpMethod, url);
        // open(request, "post", url);
        request.setRequestHeader("Content-Type", MediaType.APPLICATION_JSON);
        request.setRequestHeader("Accept", MediaType.APPLICATION_JSON);



        if (token != null)
        {
            request.setRequestHeader("Authorization", "Bearer " + getToken());
        }

        for (Map.Entry<String, String> additionalHeader : getAdditionalHeaders().entrySet())
        {
            final String key = additionalHeader.getKey();
            final String value = additionalHeader.getValue();
            request.setRequestHeader(key, value);
            log("Writing header " + key + ":" + value);
        }
        String requestData = body != null ? body : "";
        if (callback != null)
        {
            request.setOnReadyStateChange((XMLHttpRequest xhr) -> {
                final int readyState = xhr.getReadyState();
                if (readyState == XMLHttpRequest.DONE)
                {
                    xhr.clearOnReadyStateChange();
                    onResponseReceived(xhr, callback);
                }
            });
            request.send(requestData);
            return null;
        }
        else
        {
            request.send(requestData);
            CallbackContainer callbackContainer = new CallbackContainer();
            onResponseReceived(request, callbackContainer );
            if (callbackContainer.caught == null)
                return callbackContainer.result;
            if (callbackContainer.caught instanceof Exception)
            {
                throw (Exception) callbackContainer.caught;
            }
            throw new Exception(callbackContainer.caught);
        }
    }

    class CallbackContainer implements AsyncCallback<T>
    {
        Throwable caught;
        T result;

        @Override public void onFailure(Throwable caught)
        {
            this.caught = caught;
        }

        @Override public void onSuccess(T result)
        {
            this.result = result;
        }
    }

    public void onResponseReceived(XMLHttpRequest rsp,AsyncCallback callback)
    {
        final int sc = rsp.getStatus();
        final String responseText = rsp.getResponseText();
        final String statusText = rsp.getStatusText();
        String contentType = rsp.getResponseHeader("Content-Type");
        processResponse(sc, responseText, statusText, contentType, callback);
    }

    protected void processResponse(final int sc, final String responseText, final String statusText, String contentType,    AsyncCallback callback)
    {
        log("Response " + responseText + " for " + contentType + " Status " + statusText + " [" + sc + "]");
        if (sc == Response.SC_NO_CONTENT)
        {
            if (resultType.equals("void"))
            {
                callback.onSuccess(null);
            }
            else
            {
                callback.onFailure(new InvocationException("Expected " + resultType + " but no JSON response: " + responseText + " Status " + statusText));
            }
            return;
        }
        if (isJsonBody(contentType))
        {
            final Object parsedResult;
            try
            {
                log("Parsing " + responseText);
                if (resultDeserializer == null)
                {
                    //final String type = getPrimiteType(jsonParser, responseText);
                    if (resultType.equals("java.lang.Boolean") || resultType.equals("boolean"))
                    {
                        parsedResult = Boolean.parseBoolean(responseText);
                    }
                    else if (resultType.equals("java.lang.Double") || resultType.equals("double"))
                    {
                        parsedResult = Double.parseDouble(responseText);
                    }
                    else if (resultType.equals("java.lang.Integer") || resultType.equals("int"))
                    {
                        parsedResult = Integer.parseInt(responseText);
                    }
                    else if (resultType.equals("java.lang.Float") || resultType.equals("float"))
                    {
                        parsedResult = Float.parseFloat(responseText);
                    }
                    else if (resultType.equals("java.lang.Character") || resultType.equals("char"))
                    {
                        parsedResult = responseText.length() > 0 ? responseText.charAt(1) : null;
                    }
                    else
                    {
                        throw new IllegalStateException("Illegal response type" + resultType);
                    }
                }
                else
                {
                    parsedResult = parse(jsonParser, responseText);
                }
                log("Response parsed " + parsedResult);
            }
            catch (RuntimeException e)
            {
                callback.onFailure(new InvocationException("Bad JSON response: " + e));
                return;
            }
            log("Checking error.");
            if (sc != Response.SC_OK)
            {
                Exception e = null;
                JsonErrorResult errorResult = (JsonErrorResult) parsedResult;
                final String message = errorResult.message();
                final List<String> messages = errorResult.messages() != null ? new ArrayList<>(Arrays.asList(errorResult.messages())) : Collections.emptyList();
                ArrayList<SerializableExceptionStacktraceInformation> stacktrace = new ArrayList<>();
                final Data[] data = errorResult.data();
                if (data != null)
                {
                    for (Data ste : data)
                    {
                        stacktrace.add(new SerializableExceptionStacktraceInformation(ste.className(), ste.methodName(), ste.lineNumber(), ste.fileName()));
                    }
                }
                final String exceptionClass = errorResult.exceptionClass();
                SerializableExceptionInformation exceptionInformation = new SerializableExceptionInformation(message, exceptionClass, messages, stacktrace);
                e = exceptionDeserializer.deserializeException(exceptionInformation);
                if (e == null)
                {
                    final String errmsg = message;
                    e = new RemoteJsonException(errmsg, sc, new JSONObject(errorResult));
                }
                callback.onFailure(e);
                return;
            }
            if (sc == Response.SC_OK)
            {
                log("Successfull call. Mapping to response.");
                final T deserialzedResult;
                try
                {
                    log("Parsing " + parsedResult);
                    if (resultDeserializer == null)
                    {
                        deserialzedResult = (T) parsedResult;
                    }
                    else
                    {
                        deserialzedResult = resultDeserializer.fromJson(parsedResult);
                    }
                    log("Parsed to " + deserialzedResult);

                }
                catch (RuntimeException e)
                {
                    callback.onFailure(new InvocationException("Invalid JSON Response", e));
                    return;
                }
                callback.onSuccess(deserialzedResult);
                return;
            }
            else
            {
                log("Unsuccessfull call. Status " + sc);
            }
        }

        if (sc == Response.SC_OK)
        {
            callback.onFailure(new InvocationException("No JSON response: " + responseText + " Status " + statusText));
        }
        else
        {
            callback.onFailure(new StatusCodeException(sc, statusText));
        }
    }

    protected void log(String message)
    {
        System.out.println(message);
    }

    protected static boolean isJsonBody(String type)
    {
        if (type == null)
        {
            return false;
        }
        int semi = type.indexOf(';');
        if (semi >= 0)
        {
            type = type.substring(0, semi).trim();
        }
        return type.contains(MediaType.APPLICATION_JSON);
    }

    /**
     * Call a JSON parser javascript function to parse an encoded JSON string.
     *
     * @param parserFunction
     *            a javascript function
     * @param json
     *            encoded JSON text
     * @return the parsed data
     * @see #jsonParser
     */
    private static final native Object parse(JavaScriptObject parserFunction, String json)
    /*-{
    var o = parserFunction(json);
    return o;
  }-*/;

    private static class JsonErrorResult extends JavaScriptObject
    {
        protected JsonErrorResult()
        {
        }

        final native String message()/*-{ return this.message; }-*/;

        final native String exceptionClass()/*-{ return this.exceptionClass; }-*/;

        final native String[] messages()/*-{ return this.message; }-*/;

        final native Data[] data()/*-{ return this.data}-*/;
    }

    private static class Data extends JavaScriptObject
    {

        protected Data()
        {
        }

        final native String className()/*-{return this.className}-*/;

        final native String methodName()/*-{return this.methodName}-*/;

        final native int lineNumber()/*-{return this.lineNumber}-*/;

        final native String fileName()/*-{return this.fileName}-*/;
    }

    public static String encodeBase64(String data)
    {
        return URL.encodeQueryString(data);
    }

}
