package org.rapla.inject.scanning;

import org.scannotation.AnnotationDB;
import org.scannotation.WarUrlFinder;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WebappScannotationLoader extends ScannotationLoader
{

    ServletContext servletContext;

    public WebappScannotationLoader(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }
    @Override
    public LoadingResult loadClasses(LoadingFilter filter,Collection<Class<? extends Annotation>> annotationClasses)
    {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Set<String> classes;
        try
        {
            final Collection<URL> scanningUrls = getScanningUrlsForWebapp(servletContext);
            classes = scanWithAnnotation(scanningUrls, annotationClasses, filter);
        }
        catch (MalformedURLException e)
        {
            return new LoadingResult(e);
        }
        return loadClasses(classLoader, filter,classes);
    }

    private static URL[] findWebInfLibClasspaths(ServletContext servletContext)
    {
        ArrayList<URL> list = new ArrayList<URL>();
        Set libJars = servletContext.getResourcePaths("/WEB-INF/lib");
        if (libJars == null)
        {
            URL[] empty = {};
            return empty;
        }
        for (Object jar : libJars)
        {
            try
            {
                list.add(servletContext.getResource((String) jar));
            }
            catch (MalformedURLException e)
            {
                throw new RuntimeException(e);
            }
        }
        return list.toArray(new URL[list.size()]);
    }

    static public Collection<URL> getScanningUrlsForWebapp(ServletContext servletContext) throws MalformedURLException
    {
        Collection<URL> result = new ArrayList<URL>();
        URL[] urls = findWebInfLibClasspaths(servletContext);
        result.addAll(Arrays.asList( urls));
        URL url = WarUrlFinder.findWebInfClassesPath(servletContext);
        if (url != null)
        {
            result.add(url);
        }
        return result;
    }



}
