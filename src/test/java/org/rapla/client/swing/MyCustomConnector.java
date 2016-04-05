package org.rapla.client.swing;

import org.rapla.rest.client.gwt.MockProxy;
import org.rapla.rest.client.swing.BasicRaplaHTTPConnector;
import org.rapla.rest.client.swing.RaplaConnectException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

class MyCustomConnector implements BasicRaplaHTTPConnector.CustomConnector
{

    String accessToken;

    @Override public String reauth(BasicRaplaHTTPConnector proxy) throws Exception
    {
        return accessToken;
    }

    @Override public String getAccessToken()
    {
        return accessToken;
    }

    @Override public Exception deserializeException(String classname, String s, List<String> params)
    {
        return new Exception(classname + " " + s + " " + params);
        // throw new Au
    }

    @Override public Class[] getNonPrimitiveClasses()
    {
        return new Class[0];
    }

    @Override public Exception getConnectError(IOException ex)
    {
        return new RaplaConnectException("Connection Error " + ex.getMessage());
    }

    @Override public MockProxy getMockProxy()
    {
        return null;
    }
}
