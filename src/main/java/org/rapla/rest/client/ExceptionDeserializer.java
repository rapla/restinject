package org.rapla.rest.client;

public interface ExceptionDeserializer {

	Exception deserializeException(SerializableExceptionInformation exceptionInformation);

}
