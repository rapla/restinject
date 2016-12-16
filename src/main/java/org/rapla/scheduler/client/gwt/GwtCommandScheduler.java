package org.rapla.scheduler.client.gwt;

import org.rapla.logger.Logger;
import org.rapla.rest.client.gwt.internal.impl.AsyncCallback;
import org.rapla.rest.client.gwt.internal.impl.GwtClientServerConnector;
import org.rapla.scheduler.Cancelable;
import org.rapla.function.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.impl.UnsynchronizedPromise;

public  class GwtCommandScheduler implements CommandScheduler
{
    Logger logger;
    public GwtCommandScheduler(Logger logger)
    {
        this.logger= logger;
    }

    @Override
    public Cancelable schedule(final Command command, long delay, final long period)
    {
        if (period > 0)
        {
            SchedulerImpl.RepeatingCommand cmd = new SchedulerImpl.RepeatingCommand()
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
            SchedulerImpl.get(logger).scheduleFixedPeriod(cmd, (int) period);
        }
        else
        {
            SchedulerImpl.ScheduledCommand entry = new SchedulerImpl.ScheduledCommand()
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
            SchedulerImpl.get(logger).scheduleEntry(entry);
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
        final UnsynchronizedPromise<T> promise = new UnsynchronizedPromise<T>();
        scheduleDeferred(() ->
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
        final UnsynchronizedPromise<Void> promise = new UnsynchronizedPromise<Void>();
        scheduleDeferred(() ->
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
        final UnsynchronizedPromise<T> promise = new UnsynchronizedPromise<T>();
        // We call the proxy directly so we don't mess with the callbacks
        scheduleDeferred(() ->
        {
            GwtClientServerConnector.registerSingleThreadedCallback(new AsyncCallback<T>()
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

    private void scheduleDeferred(SchedulerImpl.ScheduledCommand cmd)
    {
        SchedulerImpl.get(logger).scheduleDeferred(cmd);
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

    protected void warn(String message, Exception e)
    {
        logger.warn( message, e);
    }

}
