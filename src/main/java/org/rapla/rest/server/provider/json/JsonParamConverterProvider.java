package org.rapla.rest.server.provider.json;

import com.google.gson.Gson;
import org.rapla.rest.JsonParserWrapper;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

@Provider public class JsonParamConverterProvider implements ParamConverterProvider
{

    private static final class JsonParamConverter<T> implements ParamConverter<T>
    {

        private Gson gson = JsonParserWrapper.defaultGsonBuilder().create();
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
        if (genericType instanceof Class && ((Class)genericType).isArray())
        {
            if (supportedClassed.contains(rawType))
            {
                return null;
            }
            return new JsonParamConverter<T>(rawType);
        }
        //        else
        if (!rawType.isPrimitive() && !String.class.isAssignableFrom(rawType))
        {
            return new JsonParamConverter<T>(genericType);
        }
        return null;
    }

}
