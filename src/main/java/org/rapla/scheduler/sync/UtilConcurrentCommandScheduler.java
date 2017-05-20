package org.rapla.scheduler.sync;

import org.rapla.logger.Logger;
import org.rapla.scheduler.Cancelable;
import org.rapla.function.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.UnsynchronizedCompletablePromise;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class UtilConcurrentCommandScheduler implements CommandScheduler, Executor
{
    private final ScheduledExecutorService scheduledExecutor;
    private final Executor promiseExecuter;
    protected final Logger logger;

    private ConcurrentHashMap<Object, CancableTask> futureTasks = new ConcurrentHashMap<Object, CancableTask>();

    public UtilConcurrentCommandScheduler(Logger logger)
    {
        this(logger, 6);
    }

    public UtilConcurrentCommandScheduler(Logger logger, int poolSize)
    {
        this.logger = logger;
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
        this.scheduledExecutor = executor;
        this.promiseExecuter = executor;
    }

    public void execute(Runnable task)
    {
        scheduledExecutor.execute(task);
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
        if (scheduledExecutor.isShutdown())
        {
            Exception ex = new Exception("Can't schedule command because executer is already shutdown " + task.toString());
            error(ex.getMessage(), ex);
            return createCancable(null);
        }

        TimeUnit unit = TimeUnit.MILLISECONDS;
        ScheduledFuture<?> schedule = scheduledExecutor.schedule(task, delay, unit);
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

    protected void error(String message, Exception ex)
    {
        logger.error(message, ex);
    }

    protected void debug(String message)
    {
        logger.debug(message);
    }

    protected void info(String message)
    {
        logger.info(message);
    }

    protected void warn(String message)
    {
        logger.warn(message);
    }

    private Cancelable schedule(Runnable task, long delay, long period)
    {
        if (scheduledExecutor.isShutdown())
        {
            Exception ex = new Exception("Can't schedule command because executer is already shutdown " + task.toString());
            error(ex.getMessage(), ex);
            return createCancable(null);
        }
        TimeUnit unit = TimeUnit.MILLISECONDS;
        ScheduledFuture<?> schedule = scheduledExecutor.scheduleWithFixedDelay(task, delay, period, unit);
        return createCancable(schedule);
    }

    @Override
    public Cancelable schedule(Command command, long delay, long period)
    {
        Runnable task = createTask(command);
        return schedule(task, delay, period);
    }


    @Override
    public  Cancelable scheduleSynchronized(Object synchronizationObject, Runnable task, long delay)
    {
        CancableTask wrapper = new CancableTask(task, delay)
        {
            @Override
            protected void replaceWithNext(CancableTask next)
            {
                futureTasks.replace(synchronizationObject, this, next);
            }

            @Override
            protected void endOfQueueReached()
            {
                synchronized (synchronizationObject)
                {
                    futureTasks.remove(synchronizationObject);
                }
            }
        };
        synchronized (synchronizationObject)
        {
            CancableTask existing = futureTasks.putIfAbsent(synchronizationObject, wrapper);
            if (existing == null)
            {
                wrapper.scheduleThis();
            }
            else
            {
                existing.pushToEndOfQueue(wrapper);
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


    /*
    public  <T> T waitFor(Promise<T> promise, int timeout) throws Throwable
    {
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<T> atomicReference = new AtomicReference<>();
        AtomicReference<Throwable> atomicReferenceE = new AtomicReference<>();
        promise.whenComplete((t, ex) -> {
            atomicReferenceE.set(ex);
            atomicReference.set(t);
            semaphore.release();
        });
        semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        final Throwable throwable = atomicReferenceE.get();
        if (throwable != null)
        {
            throw throwable;
        }
        final T t = atomicReference.get();
        return t;
    }
*/


    public void cancel()
    {
        try
        {
            info("Stopping scheduler thread.");
            List<Runnable> shutdownNow = scheduledExecutor.shutdownNow();
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
            scheduledExecutor.awaitTermination(2, TimeUnit.SECONDS);
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
        SynchronizedPromise<T> promise = new SynchronizedPromise<T>(executor, future);
        return promise;
    }

    private Promise<Void> run(final Command command, Executor executor)
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
                    future.complete(Promise.VOID);
                }
                catch (Exception ex)
                {
                    future.completeExceptionally(ex);
                }
            }
        });
        SynchronizedPromise<Void> promise = new SynchronizedPromise<Void>(executor, future);
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

    @Override
    public <T> CompletablePromise<T> createCompletable()
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        SynchronizedCompletablePromise<T> promise = new SynchronizedCompletablePromise<T>(promiseExecuter, future);
        return promise;
    }

    /*
    public <T> Promise<T> synchronizeTo(Promise<T> promise)
    {
        return synchronizeTo(promise,promiseExecuter);
    }
    /*

    /** the promise complete and exceptional methods will be called with the passed executer Consumer&lt;Runnable&gt; is the same as java.util.concurrent.Executor interface
     * You can use this to synchronize to SwingEventQueues*/
    /*
    public <T> Promise<T> synchronizeTo(Promise<T> promise, Executor executor)
    {
        final CompletablePromise<T> completablePromise = new UnsynchronizedCompletablePromise<>();
        promise.whenComplete((t, ex) ->
        {
            executor.execute(() ->
            {
                if (ex != null)
                {
                    completablePromise.completeExceptionally(ex);
                }
                else
                {
                    completablePromise.complete(t);
                }
            });
        });
        return completablePromise;
    }
    */

}