package org.rapla.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("rest")
public class RestApplication extends Application
{
    private final Set<Class<?>> classes;

    public RestApplication()
    {
        final Set<String> pathsToLookFor = new HashSet<>();
        pathsToLookFor.add("META-INF/services/javax.ws.rs.Path");
        pathsToLookFor.add("META-INF/services/javax.ws.rs.ext.Provider");
        pathsToLookFor.add("/META-INF/services/javax.ws.rs.Path");
        pathsToLookFor.add("/META-INF/services/javax.ws.rs.ext.Provider");
        final Set<Class<?>> classes = new LinkedHashSet<>();
        try
        {
            for (String pathToLookFor : pathsToLookFor)
            {
                final Enumeration<URL> resources = getClass().getClassLoader().getResources(pathToLookFor);
                while (resources.hasMoreElements())
                {
                    final URL nextElement = resources.nextElement();
                    try (final InputStream openStream = nextElement.openStream())
                    {
                        final BufferedReader br = new BufferedReader(new InputStreamReader(openStream, "UTF-8"));
                        String line = null;
                        while ((line = br.readLine()) != null)
                        {
                            try
                            {
                                Class<?> clazz = Class.forName(line);
                                if(!clazz.isInterface())
                                {
                                    classes.add(clazz);
                                }
                                else
                                {
                                    System.err.println("unknown service found: " + line);
                                }
                            }
                            catch (Throwable t)
                            {
                                t.printStackTrace();
                            }
                        }
                        br.close();
                    }
                }
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        this.classes = Collections.unmodifiableSet(classes);
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return classes;
    }
    
    @Override
    public Set<Object> getSingletons()
    {
        return super.getSingletons();
    }
}
