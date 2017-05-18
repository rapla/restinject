package org.rapla.scheduler.client.swing;

import org.rapla.logger.Logger;
import org.rapla.scheduler.Cancelable;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.UnsynchronizedCompletablePromise;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwingScheduler extends UtilConcurrentCommandScheduler
{
    private AtomicBoolean waitingInCommandThread = new AtomicBoolean(false);

    public SwingScheduler(Logger logger)
    {
        this(logger, 3);
    }

    public SwingScheduler(Logger logger, int poolsize)
    {
        super(logger);
    }

    @Override
    public <T> Promise<T> synchronizeTo(Promise<T> promise)
    {
        long index = System.currentTimeMillis();
        if ( logger.isDebugEnabled())
        {
            logger.debug("Invoking update " + index + " trace " + Arrays.asList(Thread.currentThread().getStackTrace()));
        }
        final CompletablePromise<T> completablePromise = new UnsynchronizedCompletablePromise<>();
        promise.whenComplete((t, ex) ->
        {
            if ( logger.isDebugEnabled())
            {
                logger.debug("SwingUtilities invoke later " + index + " background promise complete.");
            }
            Runnable runnable = () ->
            {
                {
                    if ( logger.isDebugEnabled())
                    {
                        logger.debug("SwingUtilities invoke later complete  " + index );
                    }
                }
                if (ex != null)
                {
                    completablePromise.completeExceptionally(ex);
                }
                else
                {
                    completablePromise.complete(t);
                }
                {
                    if ( logger.isDebugEnabled())
                    {
                        logger.debug("SwingUtilities invoke later complete notify.");
                    }
                }
            };
            if (javax.swing.SwingUtilities.isEventDispatchThread())
            {
                runnable.run();
            }
            else if (!waitingInCommandThread.get())
            {
                javax.swing.SwingUtilities.invokeLater(runnable);
            }
            else
            {
                logger.error("Possible Deadloch prevented, because a waitFor call is not completed yet. May cause gui update problems." );
                schedule(runnable, 0);
            }
        });
        return completablePromise;
    }

    @Override
    public <T> T waitFor(Promise<T> promise, int timeout) throws Exception
    {
        boolean eventDispatchThread = javax.swing.SwingUtilities.isEventDispatchThread();
        if (eventDispatchThread)
        {
            waitingInCommandThread.set( true);
        }
        try
        {
            return super.waitFor(promise, timeout);
        }
        finally
        {
            if (eventDispatchThread)
            {
                waitingInCommandThread.set(false);
            }
        }
    }

    @Override
    public Cancelable scheduleSynchronized(Object synchronizationObject, Runnable task, long delay)
    {
        Runnable swingTask = new Runnable()
        {
            @Override
            public void run()
            {
                javax.swing.SwingUtilities.invokeLater(task);
            }
        };
        return super.scheduleSynchronized(synchronizationObject, swingTask, delay);
    }
}
