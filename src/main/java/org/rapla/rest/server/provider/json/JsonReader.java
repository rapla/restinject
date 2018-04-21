package org.rapla.rest.server.provider.json;

import com.google.gson.Gson;
import org.rapla.rest.JsonParserWrapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonReader<T> implements MessageBodyReader<T>
{

    final JsonParserWrapper.JsonParser gson = JsonParserWrapper.defaultJson().get();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return true;
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException, WebApplicationException
    {
        return gson.fromJson(new InputStreamReader(entityStream,"UTF-8"), type);
    }
}
