package org.rapla.inject.scanning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ServiceInfLoader extends ScanningClassLoader
{

    @Override
    public LoadingResult loadClasses(LoadingFilter filter,Collection<Class<? extends Annotation>> annotationClasses)
    {
        String[] array = new String[annotationClasses.size()];
        int index = 0;
        for ( Class<? extends Annotation> clazz: annotationClasses)
        {
            array[index++] = clazz.getCanonicalName();
        }
        return loadClassesFromMetaInfo( filter,array);
    }

    public LoadingResult loadClassesFromServiceInfFile(LoadingFilter filter,String serviceListFile)
    {
        final Set<String> strings;
        try
        {
            strings = loadServiceNames(serviceListFile);
        }
        catch (IOException e)
        {
            return new LoadingResult(e);
        }
        return loadClassesFromMetaInfo( filter, strings.toArray(new String[] {}));
    }

    public LoadingResult loadClassesFromMetaInfo(LoadingFilter filter, String... serviceNames)
    {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> classes = null;
        try
        {
            classes = findClassnamesInMetaInf(classLoader, serviceNames);
        }
        catch (IOException e)
        {
            return new LoadingResult( e);
        }
        return loadClasses(classLoader, filter,classes);
    }

    private Set<String> findClassnamesInMetaInf(ClassLoader classLoader, String[] serviceNames) throws IOException
    {
        final Set<String> pathsToLookFor = new HashSet<>();
        for (String serviceName:serviceNames)
        {
            pathsToLookFor.add("META-INF/services/"+ serviceName);
            pathsToLookFor.add("/META-INF/services/"+ serviceName);
        }
        Set<String> classes = new LinkedHashSet<>();
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
                        classes.add( line);
                    }
                    br.close();
                }
            }
        }
        return classes;
    }

    private Set<String> loadServiceNames(String serviceList) throws IOException
    {
        Set<String> result = new TreeSet<String>();
        Collection<URL> resources = find(serviceList);
        if (resources.isEmpty())
        {
            //logger.error("Service list " + serviceList + " not found or empty.");
        }
        for (URL url : resources)
        {
            final InputStream modules = url.openStream();
            final BufferedReader br = new BufferedReader(new InputStreamReader(modules, "UTF-8"));
            String interfaceName = null;
            while ((interfaceName = br.readLine()) != null)
            {
                result.add( interfaceName);
            }
            br.close();
        }
        return result;
    }

    private Collection<URL> find(String fileWithfolder) throws IOException
    {

        List<URL> result = new ArrayList<URL>();
        Enumeration<URL> resources = this.getClass().getClassLoader().getResources(fileWithfolder);
        while (resources.hasMoreElements())
        {
            result.add(resources.nextElement());
        }
        return result;
    }

}
