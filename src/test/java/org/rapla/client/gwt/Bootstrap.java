package org.rapla.client.gwt;

import org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest;
import org.rapla.gwtjsonrpc.common.FutureResult;

import javax.inject.Inject;

public class Bootstrap
{
    private final AnnotationProcessingTest webservice;

    @Inject
    public Bootstrap(AnnotationProcessingTest webservice)
    {
        this.webservice = webservice;
    }

    public AnnotationProcessingTest.Result call(AnnotationProcessingTest.Parameter p) throws Exception
    {
        final FutureResult<AnnotationProcessingTest.Result> resultFutureResult = webservice.sayHello(p);
        final AnnotationProcessingTest.Result result = resultFutureResult.get();
        return result;
    }
}
