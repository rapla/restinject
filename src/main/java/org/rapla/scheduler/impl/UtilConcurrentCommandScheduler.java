package org.rapla.scheduler.impl;

import org.rapla.scheduler.Cancelable;
import org.rapla.scheduler.Command;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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

}