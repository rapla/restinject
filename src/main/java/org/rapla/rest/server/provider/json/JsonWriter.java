package org.rapla.rest.server.provider.json;

import org.rapla.rest.SerializableExceptionInformation;
import org.rapla.rest.JsonParserWrapper;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces({MediaType.APPLICATION_JSON})
public class JsonWriter<T> implements MessageBodyWriter<T>
{
    final JsonParserWrapper.JsonParser jsonParser = JsonParserWrapper.defaultJson().get();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType);
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException, WebApplicationException
    {
        String json;
        if (t instanceof Throwable)
        {
            json = serializeException((Throwable) t);
        }
        else
        {
            try
            {
                json = jsonParser.toJson(t);
            }
            catch ( JsonParserWrapper.WrappedJsonSerializeException ex)
            {
                throw new WebApplicationException(ex);
            }
        }
        entityStream.write(json.getBytes("UTF-8"));
    }

    private String serializeException(Throwable exception)
    {
        final SerializableExceptionInformation se = new SerializableExceptionInformation(exception);
        final String json = jsonParser.toJson(se);
        return json;
    }
}
