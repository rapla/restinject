package org.rapla.jsonrpc.client.gwt;

public interface MockProxy
{
    <T> T create(Class<T> tClass, String accessToken);
}
