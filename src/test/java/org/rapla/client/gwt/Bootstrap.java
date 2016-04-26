package org.rapla.client.gwt;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.rapla.common.AnnotationProcessingTest;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.CommandScheduler;

@Singleton
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
        final Collection<AnnotationProcessingTest.Result> resultFutureResult = webservice.sayHello(p);
        final AnnotationProcessingTest.Result result = resultFutureResult.iterator().next();
        return result;
    }

    public Promise<Collection<AnnotationProcessingTest.Result>> callAsync(final AnnotationProcessingTest.Parameter p, final CommandScheduler scheduler) throws Exception
    {
        final Promise<Collection<AnnotationProcessingTest.Result>> listPromise = scheduler.supplyProxy(()->webservice.sayHello(p));
        return listPromise;
    }
}
