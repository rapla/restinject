package org.rapla.rest.generator.internal;

public interface SerializerClasses
{
    //String JsonUtils = "com.google.gwt.core.client.JsonUtils";
    String JsonUtils = "org.rapla.rest.client.gwt.internal.impl.JsonSerializer";
    String GWT_PACKAGE = "org.rapla.rest.client.gwt.internal.impl" + ".";
    String GWT_SER_PACKAGE = "org.rapla.rest.client.gwt.internal.impl.ser" + ".";

    String GwtClientServerConnector = GWT_PACKAGE+"GwtClientServerConnector";
    String ResultDeserializer= GWT_PACKAGE+"ResultDeserializer";
    String ArrayResultDeserializer= GWT_PACKAGE+"ArrayResultDeserializer";
    String JsonSerializer= GWT_PACKAGE+"JsonSerializer";
    String JavaLangString_JsonSerializer= GWT_SER_PACKAGE+"JavaLangString_JsonSerializer";
    String JavaLangInteger_JsonSerializer= GWT_SER_PACKAGE+"JavaLangInteger_JsonSerializer";
    String PrimitiveArraySerializer= GWT_SER_PACKAGE+"PrimitiveArraySerializer";
    String StringMapSerializer= GWT_SER_PACKAGE+"StringMapSerializer";
    String EnumSerializer= GWT_SER_PACKAGE+"EnumSerializer";
    String EnumSerializer_simple= GWT_SER_PACKAGE+"EnumSerializer";
    String ObjectArraySerializer= GWT_SER_PACKAGE+"ObjectArraySerializer";
    String ListSerializer= GWT_SER_PACKAGE+"ListSerializer";
    String CollectionSerializer= GWT_SER_PACKAGE+"CollectionSerializer";
    String ObjectMapSerializer= GWT_SER_PACKAGE+"ObjectMapSerializer";
    String ObjectSerializer= GWT_SER_PACKAGE+"ObjectSerializer";
    String JavaUtilDate_JsonSerializer= GWT_SER_PACKAGE+"JavaUtilDate_JsonSerializer";
    String SetSerializer= GWT_SER_PACKAGE+"SetSerializer";
    String PrimitiveArrayResultDeserializers= GWT_SER_PACKAGE+"PrimitiveArrayResultDeserializers";

    String JsonSerializer_simple= "JsonSerializer";
    String ObjectSerializer_simple= "ObjectSerializer";
}
