package org.rapla.scheduler.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.rapla.scheduler.Cancelable;
import org.rapla.scheduler.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.ResolvedPromise;

public abstract class UtilConcurrentCommandScheduler implements CommandScheduler
{
    private final ScheduledExecutorService executor;
    private final Executor promiseExecuter;

    public UtilConcurrentCommandScheduler()
    {
        this(6);
    }

    public UtilConcurrentCommandScheduler(int poolSize)
    {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize, new ThreadFactory()
        {

            public Thread newThread(Runnable r)
            {
                Thread thread = new Thread(r);
                String name = thread.getName();
                if (name == null)
                {
                    name = "";
                }
                thread.setName("raplascheduler-" + name.toLowerCase().replaceAll("thread", "").replaceAll("-|\\[|\\]", ""));
                thread.setDaemon(true);
                return thread;
            }
        });
        this.executor = executor;
        this.promiseExecuter = executor;
    }

    public void execute(Runnable task)
    {
        schedule(task, 0);
    }

    @Override
    public Cancelable schedule(Command command, long delay)
    {
        Runnable task = createTask(command);
        return schedule(task, delay);
    }

    protected Runnable createTask(final Command command)
    {
        Runnable timerTask = new Runnable()
        {
            public void run()
            {
                try
                {
                    command.execute();
                }
                catch (Exception e)
                {
                    error(e.getMessage(), e);
                }
            }

            public String toString()
            {
                return command.toString();
            }
        };
        return timerTask;
    }

    protected Cancelable schedule(Runnable task, long delay)
    {
        if (executor.isShutdown())
        {
            Exception ex = new Exception("Can't schedule command because executer is already shutdown " + task.toString());
            error(ex.getMessage(), ex);
            return createCancable(null);
        }

        TimeUnit unit = TimeUnit.MILLISECONDS;
        ScheduledFuture<?> schedule = executor.schedule(task, delay, unit);
        return createCancable(schedule);
    }

    private Cancelable createCancable(final ScheduledFuture<?> schedule)
    {
        return new Cancelable()
        {
            public void cancel()
            {
                if (schedule != null)
                {
                    schedule.cancel(true);
                }
            }
        };
    }

    abstract protected void error(String message, Exception ex);

    abstract protected void debug(String message);

    abstract protected void info(String message);

    abstract protected void warn(String message);

    private Cancelable schedule(Runnable task, long delay, long period)
    {
        if (executor.isShutdown())
        {
            Exception ex = new Exception("Can't schedule command because executer is already shutdown " + task.toString());
            error(ex.getMessage(), ex);
            return createCancable(null);
        }
        TimeUnit unit = TimeUnit.MILLISECONDS;
        ScheduledFuture<?> schedule = executor.scheduleWithFixedDelay(task, delay, period, unit);
        return createCancable(schedule);
    }

    @Override
    public Cancelable schedule(Command command, long delay, long period)
    {
        Runnable task = createTask(command);
        return schedule(task, delay, period);
    }

    @Override
    public Cancelable scheduleSynchronized(final Object synchronizationObject, Command command, final long delay)
    {
        Runnable task = createTask( command );
        CancableTask wrapper = new CancableTask(task,delay)
        {
            @Override
            protected void replaceWithNext(CancableTask next)
            {
                futureTasks.replace( synchronizationObject , this, next);
            }
            @Override
            protected void endOfQueueReached()
            {
                synchronized (synchronizationObject)
                {
                    futureTasks.remove( synchronizationObject);
                }
            }
        };
        synchronized (synchronizationObject)
        {
            CancableTask existing = futureTasks.putIfAbsent( synchronizationObject, wrapper);
            if (existing == null)
            {
                wrapper.scheduleThis();
            }
            else
            {
                existing.pushToEndOfQueue( wrapper );
            }
            return wrapper;
        }
    }

    abstract class CancableTask implements Cancelable, Runnable
    {
        long delay;
        private Runnable task;

        volatile Thread.State status = Thread.State.NEW;
        Cancelable cancelable;
        CancableTask next;

        public CancableTask(Runnable task, long delay)
        {
            this.task = task;
            this.delay = delay;
        }

        @Override
        public void run()
        {
            try
            {
                if (status == Thread.State.NEW)
                {
                    status = Thread.State.RUNNABLE;
                    task.run();
                }
            }
            finally
            {
                status = Thread.State.TERMINATED;
                scheduleNext();
            }
        }

        @Override
        public void cancel()
        {
            if (cancelable != null && status == Thread.State.RUNNABLE)
            {
                // send interrupt if thread is running
                cancelable.cancel();
            }
            status = Thread.State.TERMINATED;
        }

        public void pushToEndOfQueue(CancableTask wrapper)
        {
            if (next == null)
            {
                next = wrapper;
            }
            else
            {
                next.pushToEndOfQueue(wrapper);
            }
        }

        public void scheduleThis()
        {
            cancelable = schedule(this, delay);
        }

        private void scheduleNext()
        {
            if (next != null)
            {
                replaceWithNext(next);
                next.scheduleThis();
            }
            else
            {
                endOfQueueReached();
            }
        }

        abstract protected void replaceWithNext(CancableTask next);

        abstract protected void endOfQueueReached();
    }

    ConcurrentHashMap<Object, CancableTask> futureTasks = new ConcurrentHashMap<Object, CancableTask>();

    /*
    public Cancelable scheduleSynchronized(final Object synchronizationObject, Runnable task, final long delay)
    {
        CancableTask wrapper = new CancableTask(task,delay)
        {
            @Override
            protected void replaceWithNext(CancableTask next)
            {
                futureTasks.replace( synchronizationObject , this, next);
            }
            @Override
            protected void endOfQueueReached()
            {
                synchronized (synchronizationObject)
                {
                    futureTasks.remove( synchronizationObject);
                }
            }
        };
        synchronized (synchronizationObject)
        {
            CancableTask existing = futureTasks.putIfAbsent( synchronizationObject, wrapper);
            if (existing == null)
            {
                wrapper.scheduleThis();
            }
            else
            {
                existing.pushToEndOfQueue( wrapper );
            }
            return wrapper;
        }
    }
    */

    public void cancel()
    {
        try
        {
            info("Stopping scheduler thread.");
            List<Runnable> shutdownNow = executor.shutdownNow();
            for (Runnable task : shutdownNow)
            {
                long delay = -1;
                if (task instanceof ScheduledFuture)
                {
                    ScheduledFuture scheduledFuture = (ScheduledFuture) task;
                    delay = scheduledFuture.getDelay(TimeUnit.SECONDS);
                }
                if (delay <= 0)
                {
                    warn("Interrupted active task " + task);
                }
            }
            executor.awaitTermination(3, TimeUnit.SECONDS);
            info("Stopped scheduler thread.");
        }
        catch (Throwable ex)
        {
            warn(ex.getMessage());
        }
        // we give the update threads some time to execute
        try
        {
            Thread.sleep(50);
        }
        catch (InterruptedException e)
        {
        }
    }

    private static class PromiseRuntimeException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public PromiseRuntimeException(Throwable cause)
        {
            super(cause);
        }

        @Override
        public synchronized Throwable getCause()
        {
            return super.getCause();
        }

    }

    static class MyPromise<T> implements Promise<T>
    {

        final Executor promiseExecuter;
        final CompletionStage f;

        MyPromise(Executor executor, CompletionStage f)
        {
            this.promiseExecuter = executor;
            this.f = f;
        }

        protected <U> Promise<U> w(CompletionStage<U> stage)
        {
            return new MyPromise<U>(promiseExecuter, stage);
        }

        protected <T> CompletionStage<T> v(final Promise<T> promise)
        {
            if (promise instanceof MyPromise)
            {
                return ((MyPromise) promise).f;
            }
            else if (promise instanceof ResolvedPromise)
            {
                CompletableFuture<T> future = new CompletableFuture<T>();
                try
                {
                    final T t = ((ResolvedPromise<T>) promise).get();
                    future.complete( t );
                }
                catch (Throwable throwable)
                {
                    future.completeExceptionally( throwable);
                }
                return future;
            }
            else
            {
                throw new IllegalArgumentException("Promise implementation " + promise.getClass() +" not supported.");
            }
        }

        @Override
        public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
        {
            final java.util.function.Function<? super T, ? extends U> fun = (t) ->
            {
                try
                {
                    return fn.apply(t);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.thenApplyAsync(fun, promiseExecuter));
        }

        @Override
        public Promise<Void> thenAccept(Consumer<? super T> fn)
        {
            final java.util.function.Consumer<T> consumer = (a) ->
            {
                try
                {
                    fn.accept(a);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.thenAcceptAsync(consumer, promiseExecuter));
        }

        @Override
        public Promise<Void> thenRun(Runnable action)
        {
            return w(f.thenRunAsync(action, promiseExecuter));
        }

        @Override
        public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
        {
            final java.util.function.BiFunction<? super T, ? super U, ? extends V> bifn = (t, u) ->
            {
                try
                {
                    return fn.apply(t, u);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            final CompletionStage<? extends U> v = v(other);
            return w(f.thenCombineAsync(v, bifn, promiseExecuter));
        }

        @Override
        public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn)
        {
            final java.util.function.BiConsumer<? super T, ? super U> biConsumer = (t, u) ->
            {
                try
                {
                    fn.accept(t, u);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            final CompletionStage<? extends U> v = v(other);
            return w(f.thenAcceptBothAsync(v, biConsumer, promiseExecuter));
        }

        @Override
        public Promise<Void> runAfterBoth(Promise<?> other, Runnable action)
        {
            return w(f.runAfterBothAsync(v(other), action, promiseExecuter));
        }

        @Override
        public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
        {
            final java.util.function.Function<? super T, ? extends U> fun = (t) ->
            {
                try
                {
                    return fn.apply(t);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.applyToEitherAsync(v(other), fun, promiseExecuter));
        }

        @Override
        public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn)
        {
            final java.util.function.Consumer<? super T> action = (t) ->
            {
                try
                {
                    fn.accept(t);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.acceptEitherAsync(v(other), action, promiseExecuter));
        }

        @Override
        public Promise<Void> runAfterEither(Promise<?> other, Runnable fn)
        {
            return w(f.runAfterEitherAsync(v(other), fn, promiseExecuter));
        }

        @Override
        public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
        {
            final java.util.function.Function<? super T, ? extends CompletionStage<U>> fun = (t) ->
            {
                try
                {
                    return v(fn.apply(t));
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.thenComposeAsync(fun, promiseExecuter));
        }

        @Override
        public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
        {
            final java.util.function.Function<Throwable, ? extends T> fun = (t) ->
            {
                try
                {
                    if (t instanceof PromiseRuntimeException)
                    {
                        return fn.apply(t.getCause());
                    }
                    return fn.apply(t);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.exceptionally(fun));
        }

        @Override
        public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn)
        {
            final java.util.function.BiConsumer<? super T, ? super Throwable> bifn = (t, u) ->
            {
                try
                {
                    fn.accept(t, u);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.whenCompleteAsync(bifn, promiseExecuter));
        }

        @Override
        public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
        {
            final java.util.function.BiFunction<? super T, Throwable, ? extends U> bifn = (t, u) ->
            {
                try
                {
                    return fn.apply(t, u);
                }
                catch (Exception e)
                {
                    throw new PromiseRuntimeException(e);
                }
            };
            return w(f.handleAsync(bifn, promiseExecuter));
        }

    }

    @Override
    public <T> Promise<T> supply(Callable<T> supplier)
    {
        return supply(supplier, promiseExecuter);
    }

    private <T> Promise<T> supply(final Callable<T> supplier, Executor executor)
    {
        if (supplier == null)
            throw new NullPointerException();
        CompletableFuture<T> future = new CompletableFuture<T>();
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                final T t;
                try
                {
                    t = supplier.call();
                }
                catch (Exception ex)
                {
                    future.completeExceptionally(ex);
                    return;
                }
                future.complete(t);
            }
        });
        MyPromise<T> promise = new MyPromise<T>(executor, future);
        return promise;
    }

    private <Void> Promise<Void> run(final Command command, Executor executor)
    {
        if (command == null)
            throw new NullPointerException();
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    command.execute();
                }
                catch (Exception ex)
                {
                    future.completeExceptionally(ex);
                }
            }
        });
        MyPromise<Void> promise = new MyPromise<Void>(executor, future);
        return promise;
    }

    @Override
    public <T> Promise<T> supplyProxy(Callable<T> supplier)
    {
        return supply(supplier, promiseExecuter);
    }

    @Override
    public Promise<Void> run(Command command)
    {
        return run(command, promiseExecuter);
    }

}