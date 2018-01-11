package org.rapla.scheduler;

import io.reactivex.functions.Action;
import org.junit.Assert;
import org.junit.Test;
import org.rapla.logger.ConsoleLogger;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CommandSchedulerTest
{
    @Test
    public void testDelay() throws InterruptedException {
        final CommandScheduler scheduler = new UtilConcurrentCommandScheduler(new ConsoleLogger());
        final long start = System.currentTimeMillis();
        final AtomicLong result = new AtomicLong();
        Action action = ()-> result.set( System.currentTimeMillis());
        scheduler.delay(action,300 );
        Thread.sleep(800);
        Assert.assertTrue(result.get() - start >= 300);
    }
}
