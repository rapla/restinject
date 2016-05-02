package org.rapla.rest.client;

public interface CustomConnector extends ExceptionDeserializer
{
    String getFullQualifiedUrl(String relativePath);
    String reauth(Class proxy) throws Exception;

    String getAccessToken();
}
