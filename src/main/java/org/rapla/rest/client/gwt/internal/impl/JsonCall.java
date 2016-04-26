// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.rapla.rest.client.gwt.internal.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
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
import org.rapla.rest.client.gwt.internal.ServerUnavailableException;
import org.rapla.rest.client.internal.JsonConstants;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonCall<T> implements RequestCallback
{
    protected static final JavaScriptObject jsonParser;
    private static AsyncCallback _callback;
    private AsyncCallback callback;

    static
    {
        jsonParser = selectJsonParser();
    }

    public static void registerSingleThreadedCallback( AsyncCallback callback)
    {
        _callback = callback;
    }
    private String httpMethod;

    /**
     * Select the most efficient available JSON parser.
     *
     * If we have a native JSON parser, present in modern browsers (FF 3.5 and
     * IE8, at time of writing), it is returned. If no native parser is found, a
     * parser function using <code>eval</code> is returned.
     *
     * This is done dynamically, not with a GWT user.agent check, because FF3.5
     * does not have a specific user.agent associated with it. Introducing a new
     * property for the presence of an ES3.1 parser is not worth it, since the
     * check is only done once anyway, and will result in significantly longer
     * compile times.
     *
     * As GWT will undoubtedly introduce support for the native JSON parser in
     * the {@link com.google.gwt.json.client.JSONParser JSONParser} class, this
     * code should be reevaluated to possibly use the GWT parser reference.
     *
     * @return a javascript function with the fastest available JSON parser
     * @see "http://wiki.ecmascript.org/doku.php?id=es3.1:json_support"
     */
    private static native JavaScriptObject selectJsonParser()
    /*-{
        if ($wnd.JSON && typeof $wnd.JSON.parse === 'function')
			return $wnd.JSON.parse;
		else
			return function(expr) {
				return eval('(' + expr + ')');
			};
    }-*/;

    protected final ExceptionDeserializer exceptionDeserializer;
    protected final String url;
    protected final String body;
    protected final ResultDeserializer<T> resultDeserializer;
    protected int attempts;

    private String token;
    private final Map<String, String> additionalHeaders;

    public JsonCall(String httpMethod, final ExceptionDeserializer exceptionDeserializer, final String url, Map<String, String> additionalHeaders,
            final String requestParams, final ResultDeserializer<T> resultDeserializer)
    {
        this.httpMethod = httpMethod;
        this.exceptionDeserializer = exceptionDeserializer;
        this.url = url;
        this.additionalHeaders = additionalHeaders;
        this.body = requestParams;
        this.resultDeserializer = resultDeserializer;
    }

    public static <T> T doInvoke(final String requestMethodType, final String url, final String body, final Map<String, String> additionalHeaders,
            final ResultDeserializer<T> ser,CustomConnector connector) throws Exception
    {
        JsonCall<T> newJsonCall = new JsonCall<>(requestMethodType,connector, url, additionalHeaders, body, ser);
        final String accessToken = connector.getAccessToken();
        if (accessToken != null)
        {
            newJsonCall.setToken(accessToken);
        }
        if (_callback != null)
        {
            try
            {
                newJsonCall.send(_callback);
            }
            finally
            {
                _callback = null;
            }
            return null;
        }
        else
        {
            return newJsonCall.sendSynchronized();
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

    public void send(AsyncCallback<T> callback)
    {
        this.callback = callback;
        send();
    }

    protected void send(RequestBuilder rb)
    {
        try
        {
            if (token != null)
            {
                rb.setHeader("Authorization", "Bearer " + token);
            }
            attempts++;
            rb.send();
        }
        catch (RequestException e)
        {
            callback.onFailure(e);
            return;
        }
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

    public T sendSynchronized() throws Exception
    {
        requestId = ++lastRequestId;

        SynchronousXHR request = (SynchronousXHR) SynchronousXHR.create();
        //request.setTimeout( (int)wait);
        request.synchronousOpen(httpMethod, url);
        // request.open(httpMethod, url);
        // open(request, "post", url);
        request.setRequestHeader("Content-Type", JsonConstants.JSONRPC20_REQ_CT);
        request.setRequestHeader("Accept", JsonConstants.JSONRPC20_ACCEPT_CTS);

        if (token != null)
        {
            request.setRequestHeader("Authorization", "Bearer " + token);
        }

        String requestData = body != null ? body : "";
        request.send(requestData);
        String contentType = request.getResponseHeader("Content-Type");
        String statusText = request.getStatusText();
        String responseText = request.getResponseText();
        int sc = request.getStatus();
        CallbackContainer callbackContainer = new CallbackContainer();
        this.callback = callbackContainer;
        processResponse(sc, responseText, statusText, contentType);
        if (callbackContainer.caught == null)
            return callbackContainer.result;
        if (callbackContainer.caught instanceof Exception)
        {
            throw (Exception) callbackContainer.caught;
        }
        throw new Exception(callbackContainer.caught);
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

    @Override public void onError(final Request request, final Throwable exception)
    {
        if (exception.getClass() == RuntimeException.class && exception.getMessage().contains("XmlHttpRequest.status"))
        {
            // GWT's XMLHTTPRequest class gives us RuntimeException when the
            // status code is unreadable from the browser. This occurs when
            // the connection has failed, e.g. the host is down.
            //
            callback.onFailure(new ServerUnavailableException());
        }
        else
        {
            callback.onFailure(exception);
        }
    }

    protected static int lastRequestId = 0;

    protected int requestId;

    @Override public void onResponseReceived(final Request req, final Response rsp)
    {
        final int sc = rsp.getStatusCode();
        final String responseText = rsp.getText();
        final String statusText = rsp.getStatusText();
        String contentType = rsp.getHeader("Content-Type");
        processResponse(sc, responseText, statusText, contentType);
    }

    protected void processResponse(final int sc, final String responseText, final String statusText, String contentType)
    {
        System.out.println("Response " + responseText + " for "  + contentType + " Status " + statusText + " [" +  sc +"]");
        if (isJsonBody(contentType))
        {
            final RpcResult r;
            try
            {
                System.out.println("Parsing " + responseText);
                r = parse(jsonParser, responseText);
                System.out.println("Response parsed " + r);
            }
            catch (RuntimeException e)
            {
                callback.onFailure(new InvocationException("Bad JSON response: " + e));
                return;
            }
            System.out.println("Checking error.");
            if (sc != Response.SC_OK)
            {

                Exception e = null;
                final String message = r.message();
                final List<String> messages = r.messages() != null ?new ArrayList<>(Arrays.asList(r.messages())) : Collections.emptyList();
                ArrayList<SerializableExceptionStacktraceInformation> stacktrace = new ArrayList<>();
                final Data[] data = r.data();
                if (data != null)
                {
                    for (Data ste : data)
                    {
                        stacktrace.add(new SerializableExceptionStacktraceInformation(ste.className(), ste.methodName(), ste.lineNumber(), ste.fileName()));
                    }
                }
                final String exceptionClass = r.exceptionClass();
                SerializableExceptionInformation exceptionInformation = new SerializableExceptionInformation(message, exceptionClass, messages, stacktrace);
                e = exceptionDeserializer.deserializeException(exceptionInformation);
                if (e == null)
                {
                    final String errmsg = message;
                    e = new RemoteJsonException(errmsg, sc, new JSONObject(r));
                }
                callback.onFailure(e);
                return;
            }
            if (sc == Response.SC_OK)
            {
                System.out.println("Successfull call. Mapping to response.");
                invoke(r);
                return;
            }
            else
            {
                System.out.println("Unsuccessfull call. Status " + sc);
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

    private void invoke(final JavaScriptObject rpcResult)
    {
        final T result;
        try
        {
            System.out.println("Parsing " + rpcResult);
            result = resultDeserializer.fromResult(rpcResult);
            System.out.println("Parsed to " + result);
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            callback.onFailure(new InvocationException("Invalid JSON Response", e));
            return;
        }
        callback.onSuccess(result);
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
        return JsonConstants.JSONRPC20_ACCEPT_CTS.contains(type);
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
    private static final native RpcResult parse(JavaScriptObject parserFunction, String json)
    /*-{
    return parserFunction(json);
  }-*/;

    private static class RpcResult extends JavaScriptObject
    {
        protected RpcResult()
        {
        }

        final native String message()/*-{ return this.message; }-*/;

        final native String exceptionClass()/*-{ return this.exceptionClass; }-*/;

        final native String[] messages()/*-{ return this.message; }-*/;

        final native int code()/*-{ return this.code; }-*/;

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

    protected void send()
    {
        requestId = ++lastRequestId;
        //final StringBuilder url = new StringBuilder(proxy.getServiceEntryPoint()+"/"+URL.encodeQueryString(this.url));
        //    url.append("?rest=2.0&method=").append(methodName);
        //    url.append("&params=").append(encodedRequestParams);
        //    url.append("&id=").append(requestId);

        final RequestBuilder rb;
        RequestBuilder.Method httpMethodType = mapRequestMethod( httpMethod);
        rb = new RequestBuilder(httpMethodType, url.toString());
        for (Map.Entry<String, String> additionalHeader : getAdditionalHeaders().entrySet())
        {
            rb.setHeader(additionalHeader.getKey(), additionalHeader.getValue());
        }
        rb.setHeader("Content-Type", JsonConstants.JSONRPC20_REQ_CT);
        rb.setHeader("Accept", JsonConstants.JSONRPC20_ACCEPT_CTS);
        rb.setCallback(this);
        if ( body != null)
        {
            rb.setRequestData(body);
        }
        send(rb);
    }

    protected <T> RequestBuilder.Method mapRequestMethod(String requestMethodType)
    {
        if (requestMethodType.equals(HttpMethod.GET))
        {
            return RequestBuilder.GET;
        }
        else if (requestMethodType.equals(HttpMethod.POST))
        {
            return RequestBuilder.POST;
        }
        else if (requestMethodType.equals(HttpMethod.PUT))
        {
            return RequestBuilder.PUT;
        }
        else if (requestMethodType.equals(HttpMethod.DELETE))
        {
            return RequestBuilder.DELETE;
        }
        else
        {
            throw new IllegalArgumentException("request method not implemented: " + requestMethodType);
        }
    }
    /**
     * Javascript base64 encoding implementation from.
     *
     * http://ecmanaut.googlecode.com/svn/trunk/lib/base64.js
     */
    public static native String encodeBase64(String data)
  /*-{
    var out = "", c1, c2, c3, e1, e2, e3, e4;
    for (var i = 0; i < data.length; ) {
      c1 = data.charCodeAt(i++);
      c2 = data.charCodeAt(i++);
      c3 = data.charCodeAt(i++);
      e1 = c1 >> 2;
      e2 = ((c1 & 3) << 4) + (c2 >> 4);
      e3 = ((c2 & 15) << 2) + (c3 >> 6);
      e4 = c3 & 63;
      if (isNaN(c2))
        e3 = e4 = 64;
      else if (isNaN(c3))
        e4 = 64;
      out += tab.charAt(e1) + tab.charAt(e2) + tab.charAt(e3) + tab.charAt(e4);
    }
    return out;
  }-*/;
}
