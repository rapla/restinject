package org.rapla.rest.client;

import org.rapla.logger.Logger;

public interface CustomConnector extends ExceptionDeserializer
{
    String getFullQualifiedUrl(String relativePath);
    String reauth(Class proxy) throws Exception;

    String getAccessToken();
    Logger getLogger();
}
