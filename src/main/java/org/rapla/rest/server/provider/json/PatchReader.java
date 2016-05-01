package org.rapla.rest.server.provider.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.rapla.rest.JsonParserWrapper;
import org.rapla.rest.PATCH;
import org.rapla.rest.server.jsonpatch.JsonMergePatch;
import org.rapla.rest.server.jsonpatch.JsonPatchException;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

@Provider @PATCH public class PatchReader implements ReaderInterceptor
{
    private UriInfo info;

    @Context public void setInfo(UriInfo info)
    {
        this.info = info;
    }

    @Override public Object aroundReadFrom(ReaderInterceptorContext readerInterceptorContext) throws IOException, WebApplicationException
    {

        // Get the resource we are being called on,
        // and find the GET method
        int paramCount = 0;
        final MultivaluedMap<String, String> pathParameters = info.getPathParameters();
        final MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        paramCount += pathParameters.size();
        paramCount += queryParameters.size();
        Object resource = info.getMatchedResources().get(0);

        Method found = null;
        for (Method next : resource.getClass().getMethods())
        {
            if (next.getAnnotation(GET.class) != null && next.getParameterCount() >= paramCount)
            {
                found = next;
                break;
            }
        }
        if (found != null)
        {

            Object unpatchedObject;
            Object[] args = new Object[found.getParameterCount()];
            final Class<?>[] parameterTypes = found.getParameterTypes();
            for (int i = 0; i < args.length; i++)
            {
                final Annotation[] annotations = found.getParameterAnnotations()[i];
                Class<?> paramType = parameterTypes[i];
                for (Annotation annotation : annotations)
                {
                    Object arg = null;
                    if (annotation.annotationType().equals(PathParam.class))
                    {
                        final String value = ((PathParam) annotation).value();
                        arg = extractArgs(paramType, pathParameters.get(value));
                    }
                    else if (annotation.annotationType().equals(QueryParam.class))
                    {
                        final String value = ((QueryParam) annotation).value();
                        arg = extractArgs(paramType, queryParameters.get(value));
                    }
                    args[i] = arg;
                }
            }
            try
            {
                unpatchedObject = found.invoke(resource, args);
            }
            catch (Exception e)
            {
                throw new WebApplicationException(e);
            }
            try
            {
                final Gson gs = JsonParserWrapper.defaultGsonBuilder().create();
                JsonElement unpatchedObjectJson = gs.toJsonTree(unpatchedObject);
                JsonElement patchElement = new JsonParser().parse(new InputStreamReader(readerInterceptorContext.getInputStream()));
                final JsonMergePatch patch = JsonMergePatch.fromJson(patchElement);
                final JsonElement patchedObjectJson = patch.apply(unpatchedObjectJson);
                final String s = gs.toJson(patchedObjectJson);
                readerInterceptorContext.setInputStream(new ByteArrayInputStream(s.getBytes()));
                return readerInterceptorContext.proceed();
            }
            catch (JsonPatchException e)
            {
                throw new WebApplicationException(Response.status(500).type("text/plain").entity(e.getMessage()).build());
            }

        }
        else
        {
            throw new IllegalArgumentException("No matching GET method on resource");
        }

    }

    public Object extractArgs(Class<?> typ, List<String> strings1)
    {
        final List<String> strings = strings1;
        if (strings.size() > 0)
        {
            String value = strings.get(0);
            if (typ.equals(String.class))
            {
                return value;
            }
            else
            {
                if (typ.equals(String.class))
                    return value;
                if (typ.equals(boolean.class) || typ.equals(Boolean.class))
                {
                    return value == null ? Boolean.FALSE:Boolean.valueOf(value);
                }
                if (value == null)
                    value = "0";
                if (typ.equals(long.class) || typ.equals(Long.class))
                    return Long.valueOf(value);
                if (typ.equals(int.class) || typ.equals(Integer.class))
                    return Integer.valueOf(value);
                if (typ.equals(double.class) || typ.equals(Double.class))
                    return Double.valueOf(value);
                return null;
                // TODO support constructor String or fromValue
            }

        }

        return null;
    }

}
