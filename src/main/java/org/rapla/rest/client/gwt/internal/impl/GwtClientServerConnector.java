package org.rapla.rest.client.gwt.internal.impl;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.rapla.function.BiConsumer;
import org.rapla.function.Consumer;
import org.rapla.logger.Logger;
import org.rapla.rest.SerializableExceptionInformation;
import org.rapla.rest.SerializableExceptionInformation.SerializableExceptionStacktraceInformation;
import org.rapla.rest.client.AsyncCallback;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.ExceptionDeserializer;
import org.rapla.rest.client.RemoteConnectException;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.UnsynchronizedCompletablePromise;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GwtClientServerConnector<T>
{
    private String httpMethod;

    protected final ExceptionDeserializer exceptionDeserializer;
    protected final String url;
    protected final String body;
    protected final ResultDeserializer<T> resultDeserializer;

    Logger logger;
    private String token;
    private final Map<String, String> additionalHeaders;
    private final String resultType;

    public GwtClientServerConnector(String httpMethod, final ExceptionDeserializer exceptionDeserializer, final String url,
            Map<String, String> additionalHeaders, final String body, final ResultDeserializer<T> resultDeserializer, String resultType, Logger logger)
    {
        this.httpMethod = httpMethod;
        this.exceptionDeserializer = exceptionDeserializer;
        this.url = url;
        this.additionalHeaders = additionalHeaders;
        this.body = body;
        this.resultDeserializer = resultDeserializer;
        this.resultType = resultType;
        this.logger = logger;
    }

    public static <T> Object doInvoke(final String requestMethodType, final String url, final Map<String, String> additionalHeaders, final String body,
            final ResultDeserializer<T> ser, String resultType, CustomConnector connector, boolean isPromise) throws Exception
    {
        GwtClientServerConnector gwtConnector = new GwtClientServerConnector<>(requestMethodType, connector, url, additionalHeaders, body, ser, resultType,
                connector.getLogger());
        final String accessToken = connector.getAccessToken();
        if (accessToken != null)
        {
            gwtConnector.setToken(accessToken);
        }
        if ( isPromise)
        {

            final CompletablePromise<T> promise = connector.createCompletable();
            AsyncCallback callback = new AsyncCallback<T>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                    promise.completeExceptionally(caught);
                }

                @Override
                public void onSuccess(T result)
                {
                    promise.complete(result);
                }
            };
            gwtConnector.send( callback);
            return promise;
        }
        else
        {
            return gwtConnector.send(null);
        }
    }

    public static native <T> T createObject()/*-{
    return new $wnd.XMLHttpRequest();
    }-*/;

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

    public T send(AsyncCallback callback) throws Exception
    {
        XMLHttpRequest request = createObject();//callback != null ? XMLHttpRequest.create():SynchronousXHR.create();
        final boolean asynchronous = callback != null;
        request.open(httpMethod, url, asynchronous);

        //    ((SynchronousXHR) request).synchronousOpen(httpMethod, url);
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
        if (asynchronous)
        {
            //            request.setOnReadyStateChange((XMLHttpRequest xhr) -> {
            //                final int readyState = xhr.getReadyState();
            //                if (readyState == 4)
            //                {
            //                    xhr.clearOnReadyStateChange();
            //                    onResponseReceived(xhr, callback);
            //                }
            //            });
            request.setOnreadystatechange(() -> {
                final int readyState = request.getReadyState();
                if (readyState == 4)
                {
                    request.setOnreadystatechange(()->{return;});
                    onResponseReceived(request, callback);
                }
            });

            log("sending request asynchronous to " + url + " with post data " + requestData);
            request.send(requestData);
            return null;
        }
        else
        {
            log("sending request synchronous to " + url + " with post data " + requestData);
            request.send(requestData);
            CallbackContainer callbackContainer = new CallbackContainer();
            onResponseReceived(request, callbackContainer);
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

    protected void onResponseReceived(XMLHttpRequest rsp, AsyncCallback callback)
    {
        final int sc = rsp.getStatus();
        final String responseText = rsp.getResponseText();
        final String statusText = rsp.getStatusText();
        String contentType = rsp.getResponseHeader("Content-Type");
        processResponse(sc, responseText, statusText, contentType, callback);
    }

    private Set<String> notAllowedNulls = new HashSet<>();

    {
        notAllowedNulls.add("double");
        notAllowedNulls.add("int");
        notAllowedNulls.add("char");
        notAllowedNulls.add("float");
        notAllowedNulls.add("byte");
        notAllowedNulls.add("short");
        notAllowedNulls.add("boolean");
    }

    protected void processResponse(final int sc, final String responseText, final String statusText, String contentType, AsyncCallback callback)
    {
        log("Response " + responseText + " for " + contentType + " Status " + statusText + " [" + sc + "]");
        if (sc == 204)
        {
            if (notAllowedNulls.contains(resultType))
            {
                callback.onFailure(new RemoteConnectException("Expected " + resultType + " but no JSON response: " + responseText + " Status " + statusText));
            }
            else
            {
                callback.onSuccess(null);
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
                    parsedResult = parse(responseText);
                }
                log("Response parsed " + parsedResult);
            }
            catch (RuntimeException e)
            {
                callback.onFailure(new RemoteConnectException("Bad JSON response: " + e));
                return;
            }
            log("Checking error.");
            if (sc != 200)
            {
                Exception e = getError(sc, (JsonErrorResult) parsedResult);
                callback.onFailure(e);
                return;
            }
            if (sc == 200)
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
                    callback.onFailure(new RemoteConnectException("Invalid JSON Response", e));
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

        callback.onFailure(new RemoteConnectException("No JSON response: " + responseText + " Status " + statusText));
    }

    private Exception getError(int sc, JsonErrorResult parsedResult)
    {
        JsonErrorResult errorResult = parsedResult;
        final String message = errorResult.getMessage();
        ArrayList<SerializableExceptionStacktraceInformation> stacktrace = new ArrayList<>();
        final Data[] data = errorResult.getData();
        if (data != null)
        {
            for (Data ste : data)
            {
                stacktrace.add(new SerializableExceptionStacktraceInformation(ste.getClassName(), ste.getMethodName(), ste.getLineNumber(), ste.getFileName()));
            }
        }
        final String exceptionClass = errorResult.getExceptionClass();
        SerializableExceptionInformation exceptionInformation = new SerializableExceptionInformation(message, exceptionClass,  stacktrace);
        Exception e = exceptionDeserializer.deserializeException(exceptionInformation, sc);
        if (e == null)
        {
            final String errmsg = message;
            e = new RemoteConnectException(errmsg);
        }
        return e;
    }

    protected void log(String message)
    {
        logger.debug(message);
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

    private static final native Object parse(String json)
    /*-{
    var o =  $wnd.JSON.parse(json);
    return o;
  }-*/;

    @JsType(isNative = true)
    private interface JsonErrorResult
    {
        @JsProperty
        String getMessage();

        @JsProperty
        String getExceptionClass();

        @JsProperty
        Data[] getData();
    }

    @JsType(isNative = true)
    interface Data
    {
        @JsProperty
        String getClassName();

        @JsProperty
        String getMethodName();

        @JsProperty
        int getLineNumber();

        @JsProperty
        String getFileName();
    }

    public static native String encodeBase64(String decodedURLComponent) /*-{
      var regexp = /%20/g;
       return encodeURIComponent(decodedURLComponent).replace(regexp, "+");
  }-*/;

}
