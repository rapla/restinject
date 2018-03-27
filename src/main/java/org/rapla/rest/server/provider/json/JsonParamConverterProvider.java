package org.rapla.rest.server.provider.json;

import org.rapla.rest.JsonParserWrapper;
import org.rapla.rest.client.internal.isodate.ISODateTimeFormat;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Provider public class JsonParamConverterProvider implements ParamConverterProvider
{

    private static final class JsonParamConverter<T> implements ParamConverter<T>
    {

        private JsonParserWrapper.JsonParser gson = JsonParserWrapper.defaultJson().get();
        private Type genericType;

        public JsonParamConverter(Type genericType)
        {
            this.genericType = genericType;
        }

        @Override public T fromString(String value)
        {
            final T t = gson.fromJson(value, genericType);
            return t;
        }

        @Override public String toString(T value)
        {
            return gson.toJson(value);
        }

    }

    private static final class DateParamConverter implements ParamConverter<Date>
    {

        @Override public Date fromString(String value)
        {
            return (value != null ? ISODateTimeFormat.INSTANCE.parseTimestamp(value) : null);
        }

        @Override public String toString(Date value)
        {
            return value != null ? ISODateTimeFormat.INSTANCE.formatTimestamp(value) : null;
        }

    }

    public JsonParamConverterProvider()
    {
    }

    private static Set<Type> supportedClassed = new HashSet();

    static
    {
        supportedClassed.add(String.class);
        supportedClassed.add(Integer.class);
        supportedClassed.add(Double.class);
        supportedClassed.add(Float.class);
        supportedClassed.add(Character.class);
        supportedClassed.add(Long.class);
        supportedClassed.add(Boolean.class);
    }

    @Override public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, Annotation[] annotations)
    {
        if (rawType.equals(Date.class))
        {
            return (ParamConverter<T>) new DateParamConverter();
        }
        if (genericType instanceof Class && ((Class) genericType).isArray())
        {
            if (supportedClassed.contains(rawType))
            {
                return null;
            }
            return new JsonParamConverter<T>(rawType);
        }
        if (!rawType.isPrimitive() && !String.class.isAssignableFrom(rawType))
        {
            return new JsonParamConverter<T>(genericType);
        }
        return null;
    }

}
