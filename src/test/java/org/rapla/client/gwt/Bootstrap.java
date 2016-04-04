package org.rapla.client.gwt;

import java.util.List;

import javax.inject.Inject;

import org.rapla.common.AnnotationProcessingTest;

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
        final List<AnnotationProcessingTest.Result> resultFutureResult = webservice.sayHello(p);
        final AnnotationProcessingTest.Result result = resultFutureResult.get(0);
        return result;
    }
}
