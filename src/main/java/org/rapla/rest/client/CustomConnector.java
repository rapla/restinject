package org.rapla.rest.client;

import org.rapla.rest.client.gwt.MockProxy;

import java.io.IOException;

public interface CustomConnector extends ExceptionDeserializer, EntryPointFactory
{
    String reauth(Class proxy) throws Exception;

    Class[] getNonPrimitiveClasses();

    String getAccessToken();
}
