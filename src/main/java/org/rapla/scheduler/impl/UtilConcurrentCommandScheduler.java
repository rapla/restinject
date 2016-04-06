package org.rapla.scheduler.impl;

import org.rapla.scheduler.Cancelable;
import org.rapla.scheduler.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;

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

public abstract class UtilConcurrentCommandScheduler implements CommandScheduler
{
    private final ScheduledExecutorService executor;
    private final Executor promiseExecuter;
    public UtilConcurrentCommandScheduler() {
        this( 6);
    }

    public UtilConcurrentCommandScheduler( int poolSize) {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize,new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                String name = thread.getName();
                if ( name == null)
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
        schedule(task, 0 );
    }

    public Cancelable schedule(Command command, long delay)
    {
        Runnable task = createTask(command);
        return schedule(task, delay);
    }

    protected Runnable createTask(final Command command) {
        Runnable timerTask = new Runnable() {
            public void run() {
                try {
                    command.execute();
                } catch (Exception e) {
                    error( e.getMessage(), e);
                }
            }
            public String toString()
            {
                return command.toString();
            }
        };
        return timerTask;
    }


    public Cancelable schedule(Runnable task, long delay) {
        if (executor.isShutdown())
        {
            Exception ex = new Exception("Can't schedule command because executer is already shutdown " + task.toString());
            error(ex.getMessage(), ex);
            return createCancable( null);
        }

        TimeUnit unit = TimeUnit.MILLISECONDS;
        ScheduledFuture<?> schedule = executor.schedule(task, delay, unit);
        return createCancable( schedule);
    }



    private Cancelable createCancable(final ScheduledFuture<?> schedule) {
        return new Cancelable() {
            public void cancel() {
                if ( schedule != null)
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

    public Cancelable schedule(Runnable task, long delay, long period) {
        if (executor.isShutdown())
        {
            Exception ex = new Exception("Can't schedule command because executer is already shutdown " + task.toString());
            error(ex.getMessage(), ex);
            return createCancable( null);
        }
        TimeUnit unit = TimeUnit.MILLISECONDS;
        ScheduledFuture<?> schedule = executor.scheduleWithFixedDelay(task, delay, period, unit);
        return createCancable( schedule);
    }

    public Cancelable schedule(Command command, long delay, long period)
    {
        Runnable task = createTask(command);
        return schedule(task, delay, period);
    }

    abstract class  CancableTask implements Cancelable, Runnable
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
                if ( status == Thread.State.NEW)
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
            if ( cancelable != null && status == Thread.State.RUNNABLE )
            {
                // send interrupt if thread is running
                cancelable.cancel();
            }
            status = Thread.State.TERMINATED;
        }

        public void pushToEndOfQueue(CancableTask wrapper)
        {
            if ( next == null)
            {
                next = wrapper;
            }
            else
            {
                next.pushToEndOfQueue( wrapper);
            }
        }

        public void scheduleThis()
        {
            cancelable = schedule(this, delay);
        }

        private void scheduleNext()
        {
            if ( next != null)
            {
                replaceWithNext( next);
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

    ConcurrentHashMap<Object, CancableTask> futureTasks= new ConcurrentHashMap<Object, CancableTask>();

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

    public void cancel() {
        try{
            info("Stopping scheduler thread.");
            List<Runnable> shutdownNow = executor.shutdownNow();
            for ( Runnable task: shutdownNow)
            {
                long delay = -1;
                if ( task instanceof ScheduledFuture)
                {
                    ScheduledFuture scheduledFuture = (ScheduledFuture) task;
                    delay = scheduledFuture.getDelay( TimeUnit.SECONDS);
                }
                if ( delay <=0)
                {
                    warn("Interrupted active task " + task );
                }
            }
            executor.awaitTermination(3, TimeUnit.SECONDS);
            info("Stopped scheduler thread.");
        }
        catch ( Throwable ex)
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

    
    static class MyPromise<T>  implements Promise<T>
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
            return new MyPromise<U>(executor, stage);
        }

        protected <T> CompletionStage<T> v(final Promise<T> promise)
        {
            return new CompletionStage<T>()
            {
                @Override public <U> CompletionStage<U> thenApply(java.util.function.Function<? super T, ? extends U> fn)
                {
                    Function<? super T, ? extends U> fun = (s) -> fn.apply(s);
                    return v(promise.thenApply(fun));
                }

                @Override public <U> CompletionStage<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn)
                {
                    return thenApply( fn);
                }

                @Override public <U> CompletionStage<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn, Executor executor)
                {
                    return thenApply( fn);
                }

                @Override public CompletionStage<Void> thenAccept(java.util.function.Consumer<? super T> fn)
                {
                    Consumer<? super T> fun = (s) -> fn.accept(s);
                    return v(promise.thenAccept(fun));
                }

                @Override public CompletionStage<Void> thenAcceptAsync(java.util.function.Consumer<? super T> fn)
                {
                    return thenAccept( fn);
                }

                @Override public CompletionStage<Void> thenAcceptAsync(java.util.function.Consumer<? super T> fn, Executor executor)
                {
                    return thenAccept( fn);
                }

                @Override public CompletionStage<Void> thenRun(Runnable fn)
                {
                    return v( promise.thenRun( fn));
                }

                @Override public CompletionStage<Void> thenRunAsync(Runnable fn)
                {
                    return thenRun(fn);
                }

                @Override public CompletionStage<Void> thenRunAsync(Runnable fn, Executor executor)
                {
                    return thenRun(fn);
                }

                @Override public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                        java.util.function.BiFunction<? super T, ? super U, ? extends V> fn)
                {
                    BiFunction<? super T, ? super U, ? extends V> fun = (s,t) -> fn.apply( s,t);
                    Promise<? extends U> otherPromise = w( other);
                    return v( promise.thenCombine( otherPromise,fun));
                }

                @Override public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                        java.util.function.BiFunction<? super T, ? super U, ? extends V> fn)
                {
                    return thenCombine( other,fn);
                }

                @Override public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                        java.util.function.BiFunction<? super T, ? super U, ? extends V> fn, Executor executor)
                {
                    return thenCombine( other,fn);
                }

                @Override public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                        java.util.function.BiConsumer<? super T, ? super U> fn)
                {
                    Promise otherP = w( other);
                    BiConsumer<? super T, ? super U> fun = (s,t) -> fn.accept( s,t);
                    return v(promise.thenAcceptBoth( otherP, fun));
                }

                @Override public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                        java.util.function.BiConsumer<? super T, ? super U> action)
                {
                    return thenAcceptBoth( other, action);
                }

                @Override public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                        java.util.function.BiConsumer<? super T, ? super U> action, Executor executor)
                {
                    return thenAcceptBoth( other, action);
                }

                @Override public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action)
                {
                    Promise otherP = w( other);
                    return v(promise.runAfterBoth( otherP, action));
                }

                @Override public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action)
                {
                    return runAfterBoth( other,action);
                }

                @Override public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor)
                {
                    return runAfterBoth( other,action);
                }

                @Override public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn)
                {
                    Promise otherP = w( other);
                    Function<? super T,U> fun = (t) -> fn.apply( t);
                    return v(promise.applyToEither( otherP, fun));
                }

                @Override public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn)
                {
                    return applyToEither( other, fn);
                }

                @Override public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn,
                        Executor executor)
                {
                    return applyToEither( other, fn);
                }

                @Override public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, java.util.function.Consumer<? super T> fn)
                {
                    Promise otherP = w( other);
                    Consumer<? super T> fun = (t) -> fn.accept( t);
                    return v(promise.acceptEither( otherP, fun));
                }

                @Override public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action)
                {
                    return acceptEither( other,action);
                }

                @Override public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action,
                        Executor executor)
                {
                    return acceptEither( other,action);
                }

                @Override public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action)
                {
                    Promise otherP = w( other);
                    return v(promise.runAfterEither( otherP, action));
                }

                @Override public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action)
                {
                    return runAfterEither( other, action);
                }

                @Override public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor)
                {
                    return runAfterEither( other, action);
                }

                @Override public <U> CompletionStage<U> thenCompose(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn)
                {

                    Function<? super T, ? extends Promise<U>> fun = (s) -> w(fn.apply(s));
                    return v(promise.thenCompose(fun));
                }

                @Override public <U> CompletionStage<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn)
                {
                    return thenCompose( fn);
                }

                @Override public <U> CompletionStage<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn,
                        Executor executor)
                {
                    return thenCompose( fn);
                }

                @Override public CompletionStage<T> exceptionally(java.util.function.Function<Throwable, ? extends T> fn)
                {
                    Function<Throwable, ? extends T> fun = (s) -> fn.apply( s);
                    return v(promise.exceptionally( fun));
                }

                @Override public CompletionStage<T> whenComplete(java.util.function.BiConsumer<? super T, ? super Throwable> fn)
                {
                    BiConsumer<? super T, ? super Throwable> fun = (s,t) -> fn.accept( s,t);
                    return v( promise.whenComplete( fun));
                }

                @Override public CompletionStage<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> fn)
                {
                    return whenComplete( fn);
                }

                @Override public CompletionStage<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> fn, Executor executor)
                {
                    return whenComplete( fn);
                }

                @Override public <U> CompletionStage<U> handle(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn)
                {
                    BiFunction<? super T, Throwable, ? extends U>  fun = (s,t) -> fn.apply( s,t);
                    return v( promise.handle( fun));
                }

                @Override public <U> CompletionStage<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn)
                {
                    return handle( fn);
                }

                @Override public <U> CompletionStage<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn, Executor executor)
                {
                    return handle( fn);
                }

                @Override public CompletableFuture<T> toCompletableFuture()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }
        
        @Override public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
        {
            final java.util.function.Function<? super T, ? extends U> fun = (t) -> fn.apply(t);
            return w(f.thenApplyAsync( fun, promiseExecuter));
        }

        @Override public Promise<Void> thenAccept(Consumer<? super T> fn)
        {
            final java.util.function.Consumer<T> consumer = (a) -> {fn.accept(a);};
            return w(f.thenAcceptAsync(consumer,promiseExecuter));
        }

        @Override public Promise<Void> thenRun(Runnable action)
        {
            return w(f.thenRunAsync( action,promiseExecuter));
        }

        @Override public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
        {
            final java.util.function.BiFunction<? super T, ? super U, ? extends V> bifn = (t,u) -> fn.apply(t,u);
            final CompletionStage<? extends U> v = v(other);
            return w(f.thenCombineAsync(v,bifn, promiseExecuter));
        }

        @Override public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn)
        {
            final java.util.function.BiConsumer<? super T, ? super U> biConsumer = (t,u) -> {fn.accept(t,u);};
            final CompletionStage<? extends U> v = v(other);
            return w(f.thenAcceptBothAsync(v,biConsumer,promiseExecuter));
        }

        @Override public Promise<Void> runAfterBoth(Promise<?> other, Runnable action)
        {
            return w(f.runAfterBothAsync( v(other),action, promiseExecuter));
        }

        @Override public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
        {
            final java.util.function.Function<? super T, ? extends U> fun = (t) -> fn.apply(t);
            return w(f.applyToEitherAsync( v(other),fun,promiseExecuter));
        }

        @Override public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn)
        {
            final java.util.function.Consumer<? super T> action = (t) -> fn.accept(t);
            return w(f.acceptEitherAsync( v(other),action,promiseExecuter));
        }

        @Override public Promise<Void> runAfterEither(Promise<?> other, Runnable fn)
        {
            return w(f.runAfterEitherAsync( v(other),fn,promiseExecuter));
        }

        @Override public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
        {
            final java.util.function.Function<? super T, ? extends CompletionStage<U>> fun = (t) ->v(fn.apply(t));
            return w(f.thenComposeAsync(fun,promiseExecuter));
        }

        @Override public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
        {
            final java.util.function.Function<Throwable, ? extends T>  fun = (t) ->fn.apply(t);
            return w(f.exceptionally(fun));
        }

        @Override public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn)
        {
            final java.util.function.BiConsumer<? super T, ? super Throwable> bifn = (t,u) -> fn.accept(t,u);
            return w(f.whenCompleteAsync(bifn, promiseExecuter));
        }

        @Override public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
        {
            final java.util.function.BiFunction<? super T, Throwable, ? extends U> bifn = (t,u) -> fn.apply(t,u);
            return w(f.handleAsync(bifn,promiseExecuter));
        }
    }

    @Override public <U> Promise<U> supply(Supplier<U> sup)
    {
        final CompletableFuture<U> f = CompletableFuture.supplyAsync(() -> sup.get());
        MyPromise<U> promise = new MyPromise<U>(promiseExecuter, f);
        return promise;
    }

    @Override public <T> Promise<T> supplyProxy( Supplier<T> supplier)
    {
        return null;
    }

    @Override public Promise<Void> run(Runnable supplier)
    {
        final CompletableFuture<Void> f = CompletableFuture.runAsync(supplier);
        MyPromise<Void> promise = new MyPromise<Void>(promiseExecuter, f);
        return promise;
    }


    
}