package org.rapla.rest.server;

import org.rapla.rest.server.provider.json.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ServiceInfLoader
{
    static public class LoadingResult
    {
        private Set<Class<?>> classes = new LinkedHashSet<>();
        private List<Throwable> errors = new ArrayList<>();

        public Set<Class<?>> getClasses()
        {
            return classes;
        }

        public List<Throwable> getErrors()
        {
            return errors;
        }
    }

    static public LoadingResult loadClassesFromMetaInfo(ClassLoader classLoader, String... serviceNames) throws IOException
    {
        LoadingResult result = new LoadingResult();
        final Set<String> pathsToLookFor = new HashSet<>();
        for (String serviceName:serviceNames)
        {
            pathsToLookFor.add("META-INF/services/"+ serviceName);
            pathsToLookFor.add("/META-INF/services/"+ serviceName);
        }
        for (String pathToLookFor : pathsToLookFor)
        {
            final Enumeration<URL> resources = classLoader.getResources(pathToLookFor);
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
                            Class<?> clazz = classLoader.loadClass(line);
                            if(clazz.isInterface())
                            {
                                result.errors.add(new RuntimeException("interface not allowed in services: " + line));
                            }
                            else
                            {
                                result.classes.add(clazz);
                            }
                        }
                        catch (Throwable t)
                        {
                            result.errors.add( t);
                        }
                    }
                    br.close();
                }
            }
        }
        result.classes.add(JsonReader.class);
        return result;
    }

}
