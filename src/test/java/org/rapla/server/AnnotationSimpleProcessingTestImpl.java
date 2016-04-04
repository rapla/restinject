package org.rapla.server;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context=InjectionContext.server, of=AnnotationSimpleProcessingTest.class)
public class AnnotationSimpleProcessingTestImpl implements AnnotationSimpleProcessingTest
{
    @Inject
    RemoteSession session;
    private final HttpServletRequest request;

    @Inject
    public AnnotationSimpleProcessingTestImpl(@Context HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public String sayHello(String param)
    {
        return param + session.toString(request);
    }

    @Override
    public List<String> translations(String id)
    {
        final ArrayList<String> result = new ArrayList<String>();
        result.add(id);
        result.add(id + "_de");
        result.add(id + "_fr");
        return result;
    }

    @Override
    public List<String> exception()
    {
        throw new RuntimeException("Something went wrong");
    }
}