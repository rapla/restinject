package org.rapla.rest.server.provider.json;

import org.rapla.rest.JsonParserWrapper;
import org.rapla.rest.PATCH;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Provider @PATCH public class PatchReader implements ReaderInterceptor
{
    private UriInfo info;

    @Context public void setInfo(UriInfo info)
    {
        this.info = info;
    }

    static class MethodSignature
    {
        Set<String> queryParams = new HashSet<>();
        Set<String> pathParams = new HashSet<>();
        Set<String> headerParams = new HashSet<>();
        Set<String> paths = new HashSet();
        Class postParamType;
        Class returnType;

        public boolean matchesPaths(MethodSignature o2)
        {
            return paths.containsAll(o2.paths) && o2.pathParams.equals(pathParams) ;
        }

        public boolean returnTypeMatchesPostparam( MethodSignature o2)
        {
            return returnType != null && o2.postParamType != null && o2.postParamType.isAssignableFrom(returnType);
        }
    }

    @Override public Object aroundReadFrom(ReaderInterceptorContext readerInterceptorContext) throws IOException, WebApplicationException
    {

        // Get the resource we are being called on,
        // and find the GET method
        final List<Object> matchedResources = info.getMatchedResources();
        Method getMethod = null;
        Method patchMethod = null;
        if (matchedResources.size() == 0)
        {
            throw new IllegalArgumentException("No matching resource found!");
        }
        Object resource = matchedResources.get(0);
        for (Method method : resource.getClass().getMethods())
        {
            if (method.getAnnotation(PATCH.class) != null)
            {
                if (patchMethod == null)
                {
                    patchMethod = method;
                }
                else
                {
                    throw new IllegalArgumentException("Only one method with PATCH annotation per resource supported: " + resource.getClass());
                }
            }
        }
        if (patchMethod == null)
        {
            throw new IllegalStateException("PATCH method not found in  " + resource.getClass());
        }

        MethodSignature patchSignature = getMethodSignature(patchMethod);
        for (Method method : resource.getClass().getMethods())
        {
            if (method.getAnnotation(GET.class) != null)
            {
                MethodSignature getSignature = getMethodSignature(method);
                if (getSignature.matchesPaths(patchSignature) && getSignature.returnTypeMatchesPostparam( patchSignature))
                {
                    getMethod = method;
                    break;
                }
            }
        }

        if (getMethod == null)
        {
            throw new IllegalArgumentException("No matching GET method on resource");
        }
        Object unpatchedObject;
        Object[] args = new Object[getMethod.getParameterCount()];
        final Class<?>[] parameterTypes = getMethod.getParameterTypes();
        final MultivaluedMap<String, String> pathParameters = info.getPathParameters();
        final MultivaluedMap<String, String> queryParameters = info.getQueryParameters();
        for (int i = 0; i < args.length; i++)
        {
            final Annotation[] annotations = getMethod.getParameterAnnotations()[i];
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
                else if (annotation.annotationType().equals(HeaderParam.class))
                {
                    final String value = ((HeaderParam) annotation).value();
                    arg = extractArgs(paramType, queryParameters.get(value));
                }
                args[i] = arg;
            }
        }
        try
        {
            unpatchedObject = getMethod.invoke(resource, args);
        }
        catch (Exception e)
        {
            throw new WebApplicationException(e);
        }
        try
        {
            final InputStreamReader json = new InputStreamReader(readerInterceptorContext.getInputStream());
            final String s = JsonParserWrapper.defaultJson().get().patch(unpatchedObject, json);
            readerInterceptorContext.setInputStream(new ByteArrayInputStream(s.getBytes()));
            return readerInterceptorContext.proceed();
        }
        catch (Exception e)
        {
            throw new WebApplicationException(Response.status(500).type("text/plain").entity(e.getMessage()).build());
        }
    }

    public MethodSignature getMethodSignature(Method method)
    {
        MethodSignature result = new MethodSignature();
        for (Annotation annotation : method.getAnnotations())
        {
            if (annotation.annotationType().equals(Path.class))
            {
                result.paths.add(((Path) annotation).value());
            }
        }
        result.returnType = method.getReturnType();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++)
        {
            final Annotation[] annotations = method.getParameterAnnotations()[i];
            boolean foundAnnotation = false;
            for (Annotation annotation : annotations)
            {
                boolean found = true;
                final Class<? extends Annotation> aClass = annotation.annotationType();
                if (aClass.equals(PathParam.class))
                {
                    final String value = ((PathParam) annotation).value();
                    result.pathParams.add(value);
                }
                else if (aClass.equals(QueryParam.class))
                {
                    final String value = ((QueryParam) annotation).value();
                    result.queryParams.add(value);
                }
                else if (aClass.equals(HeaderParam.class))
                {
                    final String value = ((HeaderParam) annotation).value();
                    result.headerParams.add(value);
                }
                else if (aClass.equals(Context.class))
                {
                }
                else
                {
                    found = false;
                }
                if ( found )
                {
                    foundAnnotation = true;
                }
            }
            if ( !foundAnnotation)
            {
                result.postParamType = parameterTypes[i];
            }
        }
        return result;
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
                    return value == null ? Boolean.FALSE : Boolean.valueOf(value);
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
