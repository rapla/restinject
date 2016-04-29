package org.rapla.rest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.rapla.rest.client.internal.isodate.ISODateTimeFormat;

import javax.inject.Provider;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JsonParserWrapper
{

    /** Create a default GsonBuilder with some extra types defined. */
    public static GsonBuilder defaultGsonBuilder()
    {
        final GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(Set.class, new InstanceCreator<Set<Object>>()
        {
            @Override public Set<Object> createInstance(final Type arg0)
            {
                return new LinkedHashSet<Object>();
            }
        });
        Map<Type, InstanceCreator<?>> instanceCreators = new LinkedHashMap<Type, InstanceCreator<?>>();
        instanceCreators.put(Map.class, new InstanceCreator<Map>()
        {
            public Map createInstance(Type type)
            {
                return new LinkedHashMap();
            }
        });
        instanceCreators.put(Set.class, new InstanceCreator<Set>()
        {
            public Set createInstance(Type type)
            {
                return new LinkedHashSet();
            }
        });
        ConstructorConstructor constructorConstructor = new ConstructorConstructor(instanceCreators);
        FieldNamingStrategy fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
        Excluder excluder = Excluder.DEFAULT;
        final ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory = new ReflectiveTypeAdapterFactory(constructorConstructor, fieldNamingPolicy, excluder);
        gb.registerTypeAdapterFactory(new MapTypeAdapterFactory(constructorConstructor, false));
        gb.registerTypeAdapterFactory(new MyAdaptorFactory(reflectiveTypeAdapterFactory));
        gb.registerTypeAdapter(Date.class, new GmtDateTypeAdapter());

        GsonBuilder configured = gb.disableHtmlEscaping();
        return configured;
    }

    public static Provider<JsonParser> defaultJson()
    {
        return new Provider<JsonParser>()
        {
            GsonBuilder builder = defaultGsonBuilder();
            @Override public JsonParser get()
            {
                return new JsonParser()
                {
                    Gson gson = builder.create();
                    @Override public String toJson(Object object)
                    {
                        return gson.toJson(object);
                    }

                    @Override public Object fromJson(String json, Class clazz)
                    {
                        return gson.fromJson(json, clazz);
                    }
                };
            }
        };
    }

    public static class GmtDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date>
    {

        private GmtDateTypeAdapter()
        {
        }

        @Override public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext)
        {
            String timestamp = ISODateTimeFormat.INSTANCE.formatTimestamp(date);
            return new JsonPrimitive(timestamp);
        }

        @Override public synchronized Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext)
        {
            String asString = jsonElement.getAsString();
            try
            {
                Date timestamp = ISODateTimeFormat.INSTANCE.parseTimestamp(asString);
                return timestamp;
            }
            catch (Exception e)
            {
                throw new JsonSyntaxException(asString, e);
            }
        }
    }

    public static class MyAdaptorFactory implements TypeAdapterFactory
    {
        ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory;

        public MyAdaptorFactory(ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory)
        {
            this.reflectiveTypeAdapterFactory = reflectiveTypeAdapterFactory;
        }

        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
        {
            Class<? super T> raw = type.getRawType();
            if (GenericObjectSerializable.class.isAssignableFrom(raw))
            {
                return reflectiveTypeAdapterFactory.create(gson, type);
            }
            else
            {
                return null;
            }
        }
    }

    public interface JsonParser
    {
        String toJson(Object object);

        <T> T fromJson(String json, Class<T> clazz);
    }

    /**
     * Marker interface for serialization to use the generic object serialzer instead of a special one like MapSerializer or ListSerialzer
     * E.g. this is to overcome a behaviour of gson, that always use a MapSerializer when the class extends java.util.Map
     */
    public interface GenericObjectSerializable
    {
    }
}



