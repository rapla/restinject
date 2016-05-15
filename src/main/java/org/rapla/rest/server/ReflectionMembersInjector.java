package org.rapla.rest.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReflectionMembersInjector implements Injector
{
    private final Object component;
    private final Map<Class,Method> methodMap = new LinkedHashMap();

    public <C> ReflectionMembersInjector(Class<C> componentInterface,C component )
    {
        this.component = component;
        for (Method method:componentInterface.getMethods())
        {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final String name = method.getName();

            if ( parameterTypes.length == 1 && name.startsWith( "inject"))
            {
                Class injectable = parameterTypes[0];
                methodMap.put( injectable, method);
            }
        }

    }

    @Override public  void injectMembers(Object instance)
    {
        Class t = instance.getClass();
        final Method method = methodMap.get( t);
        try
        {
            method.invoke( component,instance );
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            final Throwable cause = e.getCause();
            if ( cause instanceof Exception)
            {
                throw new IllegalStateException((Exception)cause);
            }
            else if (cause instanceof Error)
            {
                throw (Error) cause;
            }
        }
    }

}
