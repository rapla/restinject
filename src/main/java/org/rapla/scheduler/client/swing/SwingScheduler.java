package org.rapla.scheduler.client.swing;

import io.reactivex.functions.Action;
import org.rapla.logger.Logger;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

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

    /*
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
                logger.error("Possible deadlock prevented. A waitFor call is not completed yet. This may cause gui update problems." );
                schedule(runnable, 0);
            }
        });
        return completablePromise;
    }
    */

    @Override
    protected void execute(Action action, CompletablePromise<Void> completablePromise) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    action.run();
                } catch (Exception e) {
                    completablePromise.completeExceptionally(e);
                    return;
                }
                completablePromise.complete( null );
            }
        };
        javax.swing.SwingUtilities.invokeLater(task);
    }
}
