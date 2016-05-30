package org.rapla.inject.scanning;

import org.scannotation.AnnotationDB;

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

public class ScannotationLoader extends ScanningClassLoader
{
    @Override
    public LoadingResult loadClasses(LoadingFilter filter,Collection<Class<? extends Annotation>> annotationClasses)
    {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Collection<URL> scanningUrls = getScanningUrlsFromClasspath(classLoader);
        final Set<String> classes;
        try
        {
            classes = scanWithAnnotation(scanningUrls, annotationClasses, filter );
        }
        catch (MalformedURLException e)
        {
            return new LoadingResult(e);
        }
        return loadClasses(classLoader, filter,classes);
    }
    static public Collection<URL> getScanningUrlsFromClasspath(ClassLoader cl) {
        Collection<URL> result = new ArrayList<> ();
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) cl).getURLs();
                result.addAll (Arrays.asList (urls));
            }
            cl = cl.getParent();
        }
        return result;
    }

    protected Set<String> scanWithAnnotation(Collection<URL> urls, Collection<Class<? extends Annotation>> annotationClasses, LoadingFilter filter) throws MalformedURLException
    {
        AnnotationDB db = new AnnotationDB();
        if ( filter != null)
        {
            String[] ignoredPackages = filter.getIgnoredPackages();
            if (ignoredPackages != null)
            {
                db.setIgnoredPackages(ignoredPackages);
            }
        }
        // only index class annotations as we don't want sub-resources being picked up in the scan
        db.setScanClassAnnotations(true);
        db.setScanFieldAnnotations(false);
        db.setScanMethodAnnotations(false);
        db.setScanParameterAnnotations(false);
        try
        {
            final URL[] urls1 = urls.toArray(new URL[] {});
            db.scanArchives(urls1);
            try
            {
                db.crossReferenceImplementedInterfaces();
                db.crossReferenceMetaAnnotations();
            }
            catch (AnnotationDB.CrossReferenceException ignored)
            {

            }

        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to scan WEB-INF for JAX-RS annotations, you must manually register your classes/resources", e);
        }
        final Map<String, Set<String>> annotationIndex = db.getAnnotationIndex();
        final Set<String> classnames  = new HashSet<>();
        for (Class<? extends Annotation> clazz: annotationClasses)
        {
            final Set<String> strings = annotationIndex.get(clazz.getName());
            classnames.addAll(strings);
        }
        return classnames;
    }



}
