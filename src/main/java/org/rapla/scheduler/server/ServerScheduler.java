package org.rapla.scheduler.server;

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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ServerScheduler implements CommandScheduler
{
    private final ScheduledExecutorService executor;

    public ServerScheduler() {
        this( 6);
    }

    public ServerScheduler( int poolSize) {
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

        final Executor executor;
        final CompletionStage f;

        MyPromise(Executor executor, CompletionStage f)
        {
            this.executor = executor;
            this.f = f;
        }

        protected <U> Promise<U> w(CompletionStage<U> stage)
        {
            return new MyPromise<U>(executor, stage);
        }

        protected <U> CompletionStage<U> v(Promise<U> stage)
        {
            throw new UnsupportedOperationException();
        }
        
        @Override public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
        {
            final java.util.function.Function<? super T, ? extends U> fun = (t) -> fn.apply(t);
            return w(f.thenApplyAsync( fun, executor));
        }

        @Override public Promise<Void> thenAccept(Consumer<? super T> action)
        {
            final java.util.function.Consumer<T> consumer = (a) -> {action.accept(a);};
            return w(f.thenAcceptAsync(consumer,executor));
        }

        @Override public Promise<Void> thenRun(Runnable action)
        {
            return w(f.thenRunAsync( action,executor));
        }

        @Override public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
        {
            final java.util.function.BiFunction<? super T, ? super U, ? extends V> bifn = (t,u) -> fn.apply(t,u);
            return w(f.thenCombineAsync( v(other),bifn, executor));
        }

        @Override public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> action)
        {
            final java.util.function.BiConsumer<? super T, ? super U> biConsumer = (t,u) -> {action.accept(t,u);};
            return w(f.thenAcceptBothAsync( v(other),biConsumer,executor));
        }

        @Override public Promise<Void> runAfterBoth(Promise<?> other, Runnable action)
        {
            return w(f.runAfterBothAsync( v(other),action, executor));
        }

        @Override public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
        {
            final java.util.function.Function<? super T, ? extends U> fun = (t) -> fn.apply(t);
            return w(f.applyToEitherAsync( v(other),fun,executor));
        }

        @Override public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> action)
        {
            throw new UnsupportedOperationException();
        }

        @Override public Promise<Void> runAfterEither(Promise<?> other, Runnable action)
        {
            throw new UnsupportedOperationException();
        }

        @Override public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
        {
            throw new UnsupportedOperationException();
        }

        @Override public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
        {
            throw new UnsupportedOperationException();
        }

        @Override public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)
        {
            throw new UnsupportedOperationException();
        }

        @Override public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override public <U> Promise<U> supply(Supplier<U> sup)
    {
        final CompletableFuture<U> f = CompletableFuture.supplyAsync(() -> sup.get());
        MyPromise<U> promise = new MyPromise<U>(executor, f);
        return promise;
    }


    @Override public Promise<Void> run(Runnable supplier)
    {
        final CompletableFuture<Void> f = CompletableFuture.runAsync(supplier);
        MyPromise<Void> promise = new MyPromise<Void>(executor, f);
        return promise;
    }

    @Override public <T, U> Promise<U> supplyProxy(T t, ProxyPromiseOperation<U, T> supplier)
    {
        return null;
    }
    
}