package org.rapla.scheduler.client.gwt;

import org.rapla.logger.Logger;
import org.rapla.scheduler.Cancelable;
import org.rapla.function.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.UnsynchronizedCompletablePromise;

public  class GwtCommandScheduler implements CommandScheduler
{
    protected  Logger logger;
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
            SchedulerImpl.get(logger).scheduleDeferred(entry);
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
        final UnsynchronizedCompletablePromise<T> promise = new UnsynchronizedCompletablePromise<T>();
        scheduleDeferred(() ->
        {
            try
            {
                final T result = supplier.call();
                promise.complete(result);
            }
            catch (Throwable ex)
            {
                promise.completeExceptionally(ex);
            }
        });
        return promise;
    }

    @Override
    public Promise<Void> run(final Command supplier)
    {
        final UnsynchronizedCompletablePromise<Void> promise = new UnsynchronizedCompletablePromise<Void>();
        scheduleDeferred(() ->
        {
            try
            {
                supplier.execute();
                promise.complete(null);
            }
            catch (Throwable ex)
            {
                promise.completeExceptionally(ex);
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

    @Override public Cancelable scheduleSynchronized(Object synchronizationObject, Runnable task, long delay)
    {
        return schedule(()->task.run(),delay);
    }

    protected void warn(String message, Exception e)
    {
        logger.warn( message, e);
    }

    @Override
    public <T> CompletablePromise<T> createCompletable()
    {
        return new UnsynchronizedCompletablePromise<>();
    }

}
