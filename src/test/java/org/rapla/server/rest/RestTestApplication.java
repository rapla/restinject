package org.rapla.server.rest;

import org.rapla.inject.scanning.RestEasyLoadingFilter;
import org.rapla.inject.scanning.ScanningClassLoader;
import org.rapla.inject.scanning.ServiceInfLoader;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class RestTestApplication extends Application
{
    private Set<Class<?>> classes;

    public RestTestApplication() throws IOException
    {
        Set<Class<? extends Annotation>> classSet = new HashSet<>();
        classSet.add(Provider.class);
        classSet.add(Path.class);
        final RestEasyLoadingFilter filter = new RestEasyLoadingFilter();
        final ScanningClassLoader scanningClassLoader = new ServiceInfLoader();
        final ServiceInfLoader.LoadingResult loadingResult = scanningClassLoader.loadClasses(filter,classSet);
        classes = loadingResult.getClasses();
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return classes;
    }


}
