package org.rapla.scheduler.client.gwt;

import org.rapla.rest.client.AsyncCallback;
import org.rapla.rest.client.gwt.AbstractJsonProxy;
import org.rapla.scheduler.Cancelable;
import org.rapla.scheduler.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;

import com.google.gwt.core.client.Scheduler;

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

    class GwtPromise<T> implements Promise<T>
    {
        T result;
        Throwable exception;
        Consumer<? super T> action;

        @Override
        public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
        {
            return null;
        }

        @Override
        public Promise<Void> thenAccept(Consumer<? super T> action)
        {
            if (result != null)
            {
                action.accept(result);
            }
            else
            {
                this.action = action;
            }
            return new GwtPromise<Void>();
        }

        @Override
        public Promise<Void> thenRun(Runnable action)
        {
            return null;
        }

        @Override
        public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
        {
            return null;
        }

        @Override
        public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> action)
        {
            return null;
        }

        @Override
        public Promise<Void> runAfterBoth(Promise<?> other, Runnable action)
        {
            return null;
        }

        @Override
        public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
        {
            return null;
        }

        @Override
        public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> action)
        {
            return null;
        }

        @Override
        public Promise<Void> runAfterEither(Promise<?> other, Runnable action)
        {
            return null;
        }

        @Override
        public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
        {
            return null;
        }

        @Override
        public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
        {
            return null;
        }

        @Override
        public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)
        {
            return null;
        }

        @Override
        public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
        {
            return null;
        }

        public void complete(T result)
        {
            this.result = result;
            if (action != null)
            {
                action.accept(result);
            }
        }

        public void abort(Throwable ex)
        {
            this.exception = ex;
        }
    }

    @Override
    public <T> Promise<T> supply(final Callable<T> supplier)
    {
        final GwtPromise<T> promise = new GwtPromise<T>();
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
    public Promise<Void> run(final Runnable supplier)
    {
        final GwtPromise<Void> promise = new GwtPromise<Void>();
        Scheduler.get().scheduleFinally(() ->
        {
            try
            {
                supplier.run();
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
        final GwtPromise<T> promise = new GwtPromise<T>();
        Scheduler.get().scheduleFinally(() ->
        {
            AbstractJsonProxy.callback = new AsyncCallback<T>()
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
            };
            try
            {
                supplier.call();
            }
            catch (Exception ex)
            {
                promise.abort(ex);
            }
            finally
            {
                AbstractJsonProxy.callback = null;
            }
        });
        return promise;
    }

    @Override
    public Cancelable schedule(Command command, long delay)
    {
        return schedule(command, delay, -1);
    }

    abstract protected void warn(String message, Exception e);

}
