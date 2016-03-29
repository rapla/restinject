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

package org.rapla.jsonrpc.client.gwt;

import java.util.Map;

import org.rapla.jsonrpc.client.EntryPointFactory;
import org.rapla.jsonrpc.client.gwt.internal.ExceptionDeserializer;
import org.rapla.jsonrpc.client.gwt.internal.impl.FutureResultImpl;
import org.rapla.jsonrpc.client.gwt.internal.impl.JsonCall;
import org.rapla.jsonrpc.client.gwt.internal.impl.JsonCall20HttpGet;
import org.rapla.jsonrpc.client.gwt.internal.impl.JsonCall20HttpPost;
import org.rapla.jsonrpc.client.gwt.internal.impl.ResultDeserializer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Base class for generated RemoteJsonService implementations.
 * <p>
 * At runtime <code>GWT.create(Foo.class)</code> returns a subclass of this
 * class, implementing the Foo and {@link ServiceDefTarget} interfaces.
 */
public abstract class AbstractJsonProxy implements ServiceDefTarget
{
    /** URL of the service implementation. */
    String url;
    private static String token;
    private static EntryPointFactory serviceEntryPointFactory;
    private static ExceptionDeserializer exceptionDeserializer;
    private String path;

    public static EntryPointFactory getServiceEntryPointFactory()
    {
        return serviceEntryPointFactory;
    }

    public static void setServiceEntryPointFactory(EntryPointFactory serviceEntryPointFactory)
    {
        AbstractJsonProxy.serviceEntryPointFactory = serviceEntryPointFactory;
    }

    public static void setExceptionDeserializer(ExceptionDeserializer exceptionDeserializer)
    {
        AbstractJsonProxy.exceptionDeserializer = exceptionDeserializer;
    }

    public ExceptionDeserializer getExceptionDeserializer()
    {
        return exceptionDeserializer;
    }

    @Override public String getServiceEntryPoint()
    {
        return url;
    }

    @Override public void setServiceEntryPoint(final String address)
    {
        url = address;
    }

    @Override public String getSerializationPolicyName()
    {
        return "jsonrpc";
    }

    @Override public void setRpcRequestBuilder(RpcRequestBuilder builder)
    {
        if (builder != null)
            throw new UnsupportedOperationException("A RemoteJsonService does not use the RpcRequestBuilder, so this method is unsupported.");
        /**
         * From the gwt docs:
         *
         * Calling this method with a null value will reset any custom behavior to
         * the default implementation.
         *
         * If builder == null, we just ignore this invocation.
         */
    }

    protected void setPath(String path)
    {
        this.path = path;
    }
    protected String getPath()
    {
        return path;
    }

    protected <T> void doInvoke(final Method requestMethodType, final String methodName, final String reqData, final Map<String, String> additionalHeaders, final ResultDeserializer<T> ser, final FutureResultImpl<T> cb)
            throws InvocationException
    {
        if (serviceEntryPointFactory != null)
        {
            Class serviceClass = getClass();
            String className = serviceClass.getName().replaceAll("_JsonProxy", "");
            url = serviceEntryPointFactory.getEntryPoint(className, getPath());
        }
        else {
            url = GWT.getModuleBaseURL() + getPath();
        }

        if (url == null)
        {
            throw new NoServiceEntryPointSpecifiedException();
        }
        JsonCall<T> newJsonCall = newJsonCall(requestMethodType, methodName, additionalHeaders, reqData, ser);
        cb.setCall(newJsonCall);
        if (token != null)
        {
            newJsonCall.setToken(token);
        }
    }

    protected <T> JsonCall<T> newJsonCall(Method requestMethodType, String methodName, Map<String, String>additionalHeaders, final String reqData, final ResultDeserializer<T> ser)
    {
        if(requestMethodType == RequestBuilder.POST)
        {
            return new JsonCall20HttpPost<>(this, methodName, additionalHeaders, reqData, ser);
        }
        else if (requestMethodType == RequestBuilder.GET)
        {
            return new JsonCall20HttpGet<>(this, methodName, additionalHeaders, reqData, ser);
        }
        else
        {
            throw new IllegalArgumentException("request method not implemented: " + requestMethodType);
        }
    }

    protected static native JavaScriptObject hostPageCacheGetOnce(String name)
  /*-{
      var r = $wnd[name];
      $wnd[name] = null;
      return r ? {result: r} : null;
  }-*/;

    protected static native JavaScriptObject hostPageCacheGetMany(String name)
  /*-{
      return $wnd[name] ? {result: $wnd[name]} : null;
  }-*/;

    public static void setAuthThoken(String token)
    {
        AbstractJsonProxy.token = token;
    }
}
