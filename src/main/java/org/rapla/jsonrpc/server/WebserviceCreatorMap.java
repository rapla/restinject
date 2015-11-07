package org.rapla.jsonrpc.server;

import java.util.Map;

public interface WebserviceCreatorMap
{
    WebserviceCreator get(String name);
    java.util.Map<String,WebserviceCreator> asMap();
}
