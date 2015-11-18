package org.rapla.server;

import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.jsonrpc.common.FutureResult;
import org.rapla.jsonrpc.common.ResultImpl;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@DefaultImplementation(of=AnnotationSimpleProcessingTest.class,context = InjectionContext.server)
public class AnnotationSimpleProcessingTestImpl implements AnnotationSimpleProcessingTest
{
    RemoteSession session;
    @Inject
    public AnnotationSimpleProcessingTestImpl(RemoteSession session)
    {
        this.session = session;
    }
    public FutureResult<String> sayHello(String param)
    {
        return new ResultImpl<String>(param + session.toString());
    }
    @Override
    public List<String> translations(String id)
    {
        final ArrayList<String> result = new ArrayList<String>();
        result.add(id);
        result.add(id+"_de");
        result.add(id+"_fr");
        return result;
    }
}