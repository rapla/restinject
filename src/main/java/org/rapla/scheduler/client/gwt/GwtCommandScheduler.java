package org.rapla.scheduler.client.gwt;

import org.rapla.rest.client.AsyncCallback;
import org.rapla.rest.client.gwt.internal.impl.JsonCall;
import org.rapla.scheduler.Cancelable;
import org.rapla.scheduler.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;

import com.google.gwt.core.client.Scheduler;
import org.rapla.scheduler.client.SingleThreadedPromise;

public abstract class GwtCommandScheduler implements CommandScheduler
{

    public GwtCommandScheduler()
    {

    }

    /*
    @Override public void execute(final Runnable command)
    {
        schedule(new Command()
        {
            @Override public void execute() throws Exception
            {
                command.run();
            }
        }, 0);
    }
    */

    @Override
    public Cancelable schedule(final Command command, long delay, final long period)
    {
        if (period > 0)
        {
            Scheduler.RepeatingCommand cmd = new Scheduler.RepeatingCommand()
            {

                @Override
                public boolean execute()
                {
                    try
                    {
                        //gwtLogger.info("Refreshing client with period " + period);
                        command.execute();
                    }
                    catch (Exception e)
                    {
                        warn(e.getMessage(), e);
                    }
                    return true;
                }
            };
            Scheduler.get().scheduleFixedPeriod(cmd, (int) period);
        }
        else
        {
            Scheduler.ScheduledCommand entry = new Scheduler.ScheduledCommand()
            {

                @Override
                public void execute()
                {
                    try
                    {
                        //gwtLogger.info("Refreshing client without period ");
                        command.execute();
                    }
                    catch (Exception e)
                    {
                        warn(e.getMessage(), e);
                    }

                }
            };
            Scheduler.get().scheduleEntry(entry);
        }

        return new Cancelable()
        {

            public void cancel()
            {
            }
        };
    }

    @Override
    public <T> Promise<T> supply(final Callable<T> supplier)
    {
        final SingleThreadedPromise<T> promise = new SingleThreadedPromise<T>();
        Scheduler.get().scheduleFinally(() ->
        {
            try
            {
                final T result = supplier.call();
                promise.complete(result);
            }
            catch (Throwable ex)
            {
                promise.abort(ex);
            }
        });
        return promise;
    }

    @Override
    public Promise<Void> run(final Command supplier)
    {
        final SingleThreadedPromise<Void> promise = new SingleThreadedPromise<Void>();
        Scheduler.get().scheduleFinally(() ->
        {
            try
            {
                supplier.execute();
                promise.complete(null);
            }
            catch (Throwable ex)
            {
                promise.abort(ex);
            }
        });
        return promise;
    }

    @Override
    public <T> Promise<T> supplyProxy(final Callable<T> supplier)
    {
        final SingleThreadedPromise<T> promise = new SingleThreadedPromise<T>();
        Scheduler.get().scheduleFinally(() ->
        {
            JsonCall.registerSingleThreadedCallback( new AsyncCallback<T>()
            {
                @Override
                public void onFailure(Throwable caught)
                {
                    promise.abort(caught);
                }

                @Override
                public void onSuccess(T result)
                {
                    promise.complete(result);
                }
            });
            try
            {
                supplier.call();
            }
            catch (Exception ex)
            {
                promise.abort(ex);
            }
        });
        return promise;
    }

    @Override
    public Cancelable schedule(Command command, long delay)
    {
        return schedule(command, delay, -1);
    }

    @Override public Cancelable scheduleSynchronized(Object synchronizationObject, Command task, long delay)
    {
        return schedule(task,delay);
    }

    abstract protected void warn(String message, Exception e);

}
