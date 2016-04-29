package org.rapla.client.gwt;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.rapla.common.ExampleService;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.CommandScheduler;

@Singleton
public class Bootstrap
{
    private final ExampleService webservice;

    @Inject
    public Bootstrap(ExampleService webservice)
    {
        this.webservice = webservice;
    }

    public ExampleService.Result call(ExampleService.Parameter p) throws Exception
    {
        final Collection<ExampleService.Result> resultFutureResult = webservice.sayHello(p);
        final ExampleService.Result result = resultFutureResult.iterator().next();
        return result;
    }

    public Promise<Collection<ExampleService.Result>> callAsync(final ExampleService.Parameter p, final CommandScheduler scheduler) throws Exception
    {
        final Promise<Collection<ExampleService.Result>> listPromise = scheduler.supplyProxy(()->webservice.sayHello(p));
        return listPromise;
    }
}
