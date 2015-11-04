package org.rapla.jsonrpc.client.gwt.internal;

import java.util.List;

public interface ExceptionDeserializer {

	Exception deserialize(String exception, String message, List<String> parameter);

}
