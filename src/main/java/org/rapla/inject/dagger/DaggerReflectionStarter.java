package org.rapla.inject.dagger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DaggerReflectionStarter
{
    public enum Scope
    {
        Server("server"),
        JavaClient("client.swing"),
        Gwt("client.gwt");

        Scope(String subpackage)
        {
            this.subpackage =subpackage;
        }
        String subpackage;

    }

    public static <T> T startWithReflection(Class<T> starterClass, Scope scope) throws Exception
    {
        return startWithReflectionAndStartupModule(starterClass,scope, null);
    }

    public static <T> T startWithReflectionAndStartupModule(Class<T> starterClass, Scope scope, Object startupModule) throws Exception
    {

        String moduleName;
        String file = "moduleDescription";
        ClassLoader classLoader = starterClass.getClassLoader();
        final InputStream resourceAsStream = classLoader.getResourceAsStream(file);
        if (resourceAsStream == null)
        {
            final String message = "Can't load module descritption file " + file;
            throw new Exception(message);
        }
        else
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream)))
            {
                moduleName = reader.readLine();
            }
        }
        if (moduleName == null || moduleName.trim().length() == 0)
        {
            final String message = "No module defined in " + file;
            throw new Exception(message);
        }

        final int i = moduleName.lastIndexOf(".");
        final String packageName = (i > 0 ? moduleName.substring(0, i + 1) : "") + scope.subpackage +".dagger";
        final String artifactName = firstCharUp(i >= 0 ? moduleName.substring(i + 1) : moduleName);
        final String componentClassName = packageName + "." + artifactName + scope + "Component";
        final String daggerComponentClassNamme = packageName + ".Dagger" + artifactName + scope + "Component";
        final String starterMethod = "get" + starterClass.getSimpleName();
        final Class<?> clazz = Class.forName(daggerComponentClassNamme);
        final Class<?> interfacClazz = Class.forName(componentClassName);
        Object builder = clazz.getMethod("builder").invoke(null);
        final Class<?> builderClazz = Class.forName(daggerComponentClassNamme + "$Builder");
        try
        {
            if ( startupModule != null)
            {
                Class startupModuleClass = startupModule.getClass();
                final String paramMethod = firstCharLow(startupModuleClass.getSimpleName());
                final Method method = builderClazz.getMethod(paramMethod, startupModuleClass);
                builder = method.invoke(builder, startupModule);
            }
            final Object component = builderClazz.getMethod("build").invoke( builder);
            final Method method1 = interfacClazz.getMethod(starterMethod);
            final Object serverUncasted = method1.invoke(component);
            T starter = starterClass.cast( serverUncasted);
            return starter;
        }
        catch (InvocationTargetException ex)
        {
            final Throwable targetException = ex.getTargetException();
            if (targetException != null && targetException instanceof Exception)
            {
                throw (Exception) targetException;
            }
            else
            {
                throw ex;
            }
        }
    }

    static private String firstCharUp(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.length() < 1)
        {
            return s;
        }
        final String result = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return result;
    }

    static private String firstCharLow(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.length() < 1)
        {
            return s;
        }
        final String result = Character.toLowerCase(s.charAt(0)) + s.substring(1);
        return result;
    }

}
