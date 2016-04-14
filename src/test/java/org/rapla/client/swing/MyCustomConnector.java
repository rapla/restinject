package org.rapla.client.swing;

import java.io.IOException;

import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.gwt.MockProxy;
import org.rapla.rest.client.swing.BasicRaplaHTTPConnector;
import org.rapla.rest.client.swing.RaplaConnectException;

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

    @Override public Exception deserializeException(SerializableExceptionInformation exceptionInformations)
    {
        return new Exception(exceptionInformations.getClass() + " " + exceptionInformations.getMessage() + " " + exceptionInformations.getMessages());
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
