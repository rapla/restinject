package org.rapla.rest.client;

import org.rapla.rest.client.gwt.MockProxy;

import java.io.IOException;

public interface CustomConnector extends ExceptionDeserializer
{
    String reauth(Class proxy) throws Exception;

    Class[] getNonPrimitiveClasses();

    Exception getConnectError(IOException ex);

    String getAccessToken();

    MockProxy getMockProxy();
}
