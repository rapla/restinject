package org.rapla.client.gwt;

import java.util.List;

import javax.inject.Inject;

import org.rapla.common.AnnotationProcessingTest;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.CommandScheduler;

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

    public Promise<List<AnnotationProcessingTest.Result>> callAsync(final AnnotationProcessingTest.Parameter p, final CommandScheduler scheduler) throws Exception
    {
        final Promise<List<AnnotationProcessingTest.Result>> listPromise = scheduler.supplyProxy(()->webservice.sayHello(p));
        return listPromise;
    }
}
