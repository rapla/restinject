package org.rapla.server.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.rapla.common.EnumJsonServiceImpl;
import org.rapla.common.ExampleWithParameterArrayImpl;
import org.rapla.common.SimpleServiceImpl;
import org.rapla.server.AnnotationProcessingTestImpl;
import org.rapla.server.AnnotationSimpleProcessingTestImpl;
import org.rapla.server.MyRestPage;
import org.rapla.server.rest.exception.RestExceptionMapper;
import org.rapla.server.rest.listener.RestDaggerListener.RaplaRestDaggerContextProvider;
import org.rapla.server.rest.provider.GsonReader;
import org.rapla.server.rest.provider.GsonWriter;
import org.rapla.server.rest.provider.WildcardWriter;
import org.rapla.server.rest.provider.XmlReader;
import org.rapla.server.rest.provider.XmlWriter;

public class RestTestApplication extends Application
{

    private Set<Class<?>> classes;

    public RestTestApplication()
    {
        final HashSet<Class<?>> classes = new HashSet<>();
        classes.add(GsonReader.class);
        classes.add(GsonWriter.class);
        classes.add(WildcardWriter.class);
        classes.add(XmlReader.class);
        classes.add(XmlWriter.class);
        classes.add(MyRestPage.class);
        classes.add(AnnotationSimpleProcessingTestImpl.class);
        classes.add(AnnotationProcessingTestImpl.class);
        classes.add(SimpleServiceImpl.class);
        classes.add(EnumJsonServiceImpl.class);
        classes.add(ExampleWithParameterArrayImpl.class);
        classes.add(RaplaRestDaggerContextProvider.class);
        classes.add(RestExceptionMapper.class);
        this.classes = Collections.unmodifiableSet(classes);
    }

    @Override
    public Set<Class<?>> getClasses()
    {
        return classes;
    }

}
