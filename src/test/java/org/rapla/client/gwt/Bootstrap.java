package org.rapla.client.gwt;

import org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest;
import org.rapla.gwtjsonrpc.common.FutureResult;

import javax.inject.Inject;
import java.util.List;

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
        final FutureResult<List<AnnotationProcessingTest.Result>> resultFutureResult = webservice.sayHello(p);
        final AnnotationProcessingTest.Result result = resultFutureResult.get().get(0);
        return result;
    }
}
