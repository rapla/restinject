package org.rapla.inject.jsonrpc.proxy;

import org.rapla.jsonrpc.client.swing.BasicRaplaHTTPConnector;

public interface SerializerClasses
{
    String FutureResult = org.rapla.jsonrpc.common.FutureResult.class.getCanonicalName();

    String JsonCall20HttpGet = org.rapla.jsonrpc.client.gwt.internal.impl.JsonCall20HttpGet.class.getCanonicalName();
    String JsonCall20HttpPost = org.rapla.jsonrpc.client.gwt.internal.impl.JsonCall20HttpPost.class.getCanonicalName();
    String AbstractJsonJavaProxy = BasicRaplaHTTPConnector.class.getCanonicalName();
    String AbstractJsonProxy = org.rapla.jsonrpc.client.gwt.AbstractJsonProxy.class.getCanonicalName();
    String AbstractJsonProxy_simple = org.rapla.jsonrpc.client.gwt.AbstractJsonProxy.class.getSimpleName();
    String FutureResultImpl = org.rapla.jsonrpc.client.gwt.internal.impl.FutureResultImpl.class.getCanonicalName();

    String ResultDeserializer = org.rapla.jsonrpc.client.gwt.internal.impl.ResultDeserializer.class.getCanonicalName();
    String ArrayResultDeserializer = org.rapla.jsonrpc.client.gwt.internal.impl.ArrayResultDeserializer.class.getCanonicalName();

    String JsonSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.JsonSerializer.class.getCanonicalName();
    String JsonSerializer_simple = org.rapla.jsonrpc.client.gwt.internal.impl.JsonSerializer.class.getSimpleName();

    String JavaLangString_JsonSerializer= org.rapla.jsonrpc.client.gwt.internal.impl.ser.JavaLangString_JsonSerializer.class.getCanonicalName();
    String JavaLangInteger_JsonSerializer= org.rapla.jsonrpc.client.gwt.internal.impl.ser.JavaLangInteger_JsonSerializer.class.getCanonicalName();
    String PrimitiveArraySerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.PrimitiveArraySerializer.class.getCanonicalName();
    String StringMapSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.StringMapSerializer.class.getCanonicalName();
    String EnumSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.EnumSerializer.class.getCanonicalName();
    String EnumSerializer_simple = org.rapla.jsonrpc.client.gwt.internal.impl.ser.EnumSerializer.class.getSimpleName();
    String ObjectArraySerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.ObjectArraySerializer.class.getCanonicalName();
    String ListSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.ListSerializer.class.getCanonicalName();
    String ObjectMapSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.ObjectMapSerializer.class.getCanonicalName();
    String ObjectSerializer_simple = org.rapla.jsonrpc.client.gwt.internal.impl.ser.ObjectSerializer.class.getSimpleName();
    String ObjectSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.ObjectSerializer.class.getCanonicalName();
    String JavaUtilDate_JsonSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.JavaUtilDate_JsonSerializer.class.getCanonicalName();
    String SetSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.SetSerializer.class.getCanonicalName();
    String PrimitiveArrayResultDeserializers = org.rapla.jsonrpc.client.gwt.internal.impl.ser.PrimitiveArrayResultDeserializers.class.getCanonicalName();
    String PrimitiveResultDeserializers = org.rapla.jsonrpc.client.gwt.internal.impl.ser.PrimitiveResultDeserializers.class.getCanonicalName();
    String VoidResult_JsonSerializer = org.rapla.jsonrpc.client.gwt.internal.impl.ser.VoidResult_JsonSerializer.class.getCanonicalName();


}
