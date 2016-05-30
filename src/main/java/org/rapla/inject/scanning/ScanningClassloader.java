package org.rapla.inject.scanning;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

abstract public class ScanningClassLoader
{
    public  LoadingResult loadClasses(ClassLoader classLoader, LoadingFilter filter,Set<String> classes)
    {
        LoadingResult result = new LoadingResult();
        for ( String classname:classes)
        {
            if ( filter != null && !filter.classNameShouldBeIgnored( classname))
            {
                continue;
            }
            try
            {
                Class<?> clazz;
                try
                {
                    clazz = classLoader.loadClass(classname);
                }
                catch (ClassNotFoundException e1)
                {
                    final int i = classname.lastIndexOf(".");
                    if (i >= 0)
                    {
                        StringBuilder builder = new StringBuilder(classname);
                        final StringBuilder innerClass = builder.replace(i, i + 1, "$");
                        final String innerClassName = innerClass.toString();
                        clazz = classLoader.loadClass(innerClassName);
                        result.classes.add(clazz);
                    }
                    else
                    {
                        result.errors.add(new ClassNotFoundException("Found interfaceName definition but no class for " + classname));
                        continue;
                    }
                }

                if (clazz.isInterface())
                {
                    result.errors.add(new ClassNotFoundException("interface not allowed in services: " + classname));
                }
                else
                {
                    result.classes.add(clazz);
                }
            }
            catch (Throwable t)
            {
                result.errors.add(t);
            }
        }
        return result;
    }

    abstract public LoadingResult loadClasses(LoadingFilter filter,Collection<Class<? extends Annotation>> annotationClasses);

    public interface LoadingFilter
    {
        boolean classNameShouldBeIgnored(String classname);
        String[] getIgnoredPackages();
    }

    static public class LoadingResult
    {
        private Set<Class<?>> classes = new LinkedHashSet<>();
        private List<Throwable> errors = new ArrayList<>();

        protected LoadingResult()
        {

        }

        protected LoadingResult(Exception e)
        {
            errors.add( e);
        }

        public Set<Class<?>> getClasses()
        {
            return classes;
        }

        public List<Throwable> getErrors()
        {
            return errors;
        }
    }
}
