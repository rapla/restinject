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

package org.rapla.rest.client.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import org.rapla.rest.client.AsyncCallback;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.EntryPointFactory;
import org.rapla.rest.client.ExceptionDeserializer;
import org.rapla.rest.client.gwt.internal.impl.JsonCall;
import org.rapla.rest.client.gwt.internal.impl.JsonCall20HttpGet;
import org.rapla.rest.client.gwt.internal.impl.JsonCall20HttpPost;
import org.rapla.rest.client.gwt.internal.impl.ResultDeserializer;

import javax.ws.rs.HttpMethod;
import java.util.Map;

/**
 * Base class for generated RemoteJsonService implementations.
 * <p>
 * At runtime <code>GWT.create(Foo.class)</code> returns a subclass of this
 * class, implementing the Foo and {@link ServiceDefTarget} interfaces.
 */
public abstract class AbstractJsonProxy implements ServiceDefTarget
{
    public static AsyncCallback callback;
    /** URL of the service implementation. */
    String url;
    private static String token;
    private static EntryPointFactory serviceEntryPointFactory;
    private static ExceptionDeserializer exceptionDeserializer;
    private String path;
    final private CustomConnector connector;

    public AbstractJsonProxy(CustomConnector connector)
    {
        this.connector = connector;
    }

    public MockProxy getMockProxy()
    {
        return connector.getMockProxy();
    }

    public String getMockAccessToken()
    {
        return connector.getAccessToken();
    }

    public boolean isMock()
    {
        return connector.getMockProxy() != null;
    }



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

    public String getServiceEntryPoint()
    {
        return url;
    }

    @Override public void setServiceEntryPoint(final String address)
    {
        url = address;
    }

    @Override public String getSerializationPolicyName()
    {
        return "rest";
    }

    @Override public void setRpcRequestBuilder(RpcRequestBuilder builder)
    {
        if (builder != null)
            throw new UnsupportedOperationException("A RemoteJsonService does not use the RpcRequestBuilder, so this method is unsupported.");
    }

    protected void setPath(String path)
    {
        this.path = path;
    }
    protected String getPath()
    {
        return path;
    }



    protected <T> T doInvoke(final String requestMethodType, final String path, final String reqData, final Map<String, String> additionalHeaders, final ResultDeserializer<T> ser)
            throws Exception
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
            throw new ServiceDefTarget.NoServiceEntryPointSpecifiedException();
        }
        JsonCall<T> newJsonCall = newJsonCall(requestMethodType, path, additionalHeaders, reqData, ser);
        if (token != null)
        {
            newJsonCall.setToken(token);
        }
        if ( callback != null)
        {
            newJsonCall.send( callback );
            callback = null;
            return null;
        }
        else
        {
            return newJsonCall.sendSynchronized();
        }
    }

    protected <T> JsonCall<T> newJsonCall(String requestMethodType, String methodName, Map<String, String>additionalHeaders, final String reqData, final ResultDeserializer<T> ser)
    {
        if(requestMethodType.equals(HttpMethod.GET))
        {
            return new JsonCall20HttpGet<>(this, methodName, additionalHeaders, reqData, ser);
        }
        else if(requestMethodType.equals(HttpMethod.POST))
        {
            return new JsonCall20HttpPost<>(this, methodName, additionalHeaders, reqData, ser);
        }
        else
        {
            throw new IllegalArgumentException("request method not implemented: " + requestMethodType);
        }
    }

    public static void setAuthThoken(String token)
    {
        AbstractJsonProxy.token = token;
    }
}
