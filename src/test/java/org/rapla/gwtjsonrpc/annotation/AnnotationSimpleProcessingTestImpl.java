package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.ResultImpl;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.server.RequestScoped;
import org.rapla.server.RemoteSession;

import javax.inject.Inject;

@RequestScoped
@DefaultImplementation(of=AnnotationSimpleProcessingTest.class,context = InjectionContext.server)
public class AnnotationSimpleProcessingTestImpl implements  AnnotationSimpleProcessingTest
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

}