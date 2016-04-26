package org.rapla.rest.client;

public interface CustomConnector extends ExceptionDeserializer, EntryPointFactory
{
    String reauth(Class proxy) throws Exception;

    Class[] getNonPrimitiveClasses();

    String getAccessToken();
}
