package org.rapla.gwtjsonrpc.common;

import com.google.gson.*;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.rapla.gwtjsonrpc.common.isodate.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.*;

public class JSONParserWrapper {

	/** Create a default GsonBuilder with some extra types defined. */
	  public static GsonBuilder defaultGsonBuilder(final Class[] nonPrimitiveClasses) {
	    final GsonBuilder gb = new GsonBuilder();
	    gb.registerTypeAdapter(Set.class,
	        new InstanceCreator<Set<Object>>() {
	          @Override
	          public Set<Object> createInstance(final Type arg0) {
	            return new LinkedHashSet<Object>();
	          }
	        });
	    Map<Type, InstanceCreator<?>> instanceCreators = new LinkedHashMap<Type,InstanceCreator<?>>();
	    instanceCreators.put(Map.class, new InstanceCreator<Map>() {
            public Map createInstance(Type type) {
                return new LinkedHashMap();
            }
        });
		ConstructorConstructor constructorConstructor = new ConstructorConstructor(instanceCreators);
	    FieldNamingStrategy fieldNamingPolicy = FieldNamingPolicy.IDENTITY;
	    Excluder excluder = Excluder.DEFAULT;
	    final ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory = new ReflectiveTypeAdapterFactory(constructorConstructor, fieldNamingPolicy, excluder);
	    gb.registerTypeAdapterFactory(new MapTypeAdapterFactory(constructorConstructor, false));
	    gb.registerTypeAdapterFactory(new MyAdaptorFactory(reflectiveTypeAdapterFactory,nonPrimitiveClasses));
	    gb.registerTypeAdapter(Date.class, new GmtDateTypeAdapter());
	   
	    GsonBuilder configured = gb.disableHtmlEscaping().setPrettyPrinting();
	    return configured;
	  }
	  
	  
	  public static class GmtDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
			
			private GmtDateTypeAdapter() {
			}

			@Override
			public synchronized JsonElement serialize(Date date, Type type,	JsonSerializationContext jsonSerializationContext) {
				String timestamp = ISODateTimeFormat.INSTANCE.formatTimestamp(date);
				return new JsonPrimitive(timestamp);
			}

			@Override
			public synchronized Date deserialize(JsonElement jsonElement, Type type,JsonDeserializationContext jsonDeserializationContext) {
				String asString = jsonElement.getAsString();
				try {
					Date timestamp = ISODateTimeFormat.INSTANCE.parseTimestamp(asString);
					return timestamp;
				} catch (Exception e) {
					throw new JsonSyntaxException(asString, e);
				}
			}
		}

	  public static class MyAdaptorFactory implements TypeAdapterFactory
	    {
	    	ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory;
			final Class[] nonPrimitiveClasses;
	    	public MyAdaptorFactory(ReflectiveTypeAdapterFactory reflectiveTypeAdapterFactory, final Class[] nonPrimitiveClasses) {
	    		this.reflectiveTypeAdapterFactory = reflectiveTypeAdapterFactory;
				this.nonPrimitiveClasses = nonPrimitiveClasses;
	    	}

			public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
				Class<? super T> raw = type.getRawType();
				if ( nonPrimitiveClasses != null)
				{
					boolean found = false;
					for (Class nonPrimitiveClass : nonPrimitiveClasses)
					{
						if (nonPrimitiveClass.isAssignableFrom( raw))
						{
							found = true;
							break;
						}
					}
					if ( !found)
					{
						return null;
					}
				}
				return reflectiveTypeAdapterFactory.create(gson, type);
			}
	    }
	  
	

}



