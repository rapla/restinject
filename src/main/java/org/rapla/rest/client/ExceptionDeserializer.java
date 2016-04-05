package org.rapla.rest.client;

import java.util.List;

public interface ExceptionDeserializer {

	Exception deserializeException(String exception, String message, List<String> parameter);

}
