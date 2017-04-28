package org.rapla.scheduler;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.ConsoleLogger;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class SynchronisedPromiseTest
{
    UtilConcurrentCommandScheduler utilConcurrentCommandScheduler;

    @Before
    public void init()
    {
        utilConcurrentCommandScheduler = new UtilConcurrentCommandScheduler(new ConsoleLogger());
    }

    @After
    public void destroy()
    {
        utilConcurrentCommandScheduler.cancel();
        ;
    }

    public static <T> T waitFor(Promise<T> promise, int timeout) throws Throwable
    {
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<T> atomicReference = new AtomicReference<>();
        AtomicReference<Throwable> atomicReferenceE = new AtomicReference<>();
        promise.whenComplete((t, ex) ->
        {
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

    @Test
    public void testAccept1CompletableFuture() throws Exception
    {
        String text = "Hello World";
        Promise<String> promise = utilConcurrentCommandScheduler.supply(() ->
        {
            Thread.sleep(500);
            return text;
        });
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenAccept((t) ->
        {
            acceptedText.set(t);
            semaphore.release();
        });

        AtomicReference<String> acceptedText2 = new AtomicReference<String>();
        promise.thenAccept((t) ->
        {
            acceptedText2.set(t);
            semaphore.release();
        });
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
        TestCase.assertEquals(text, acceptedText2.get());
    }

    @Test
    public void testComplete() throws Throwable
    {
        String text = "Hello World";
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        Promise<String> promise = utilConcurrentCommandScheduler.supply(() ->
        {
            Thread.sleep(500);
            return text;
        }).whenComplete((string, ex) ->
        {
            acceptedText.set(text);
        }).whenComplete((s,e) -> System.out.println(s + " " + e));
        waitFor(promise, 1000);
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test
    public void testJavaPromiseOnComplete()
    {
        final RuntimeException expectedException = new RuntimeException("Bla");

        final CompletableFuture<String> suppliedFuture = new CompletableFuture<>();
        suppliedFuture.complete("Dummy");
        CompletableFuture<String> future = suppliedFuture.thenApply((s) ->
        {
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            if (true)
            {
                throw expectedException;
            }
            return "Dummy";
        });
        final AtomicReference<Throwable> result = new AtomicReference<Throwable>();
        final CompletableFuture<String> completableFuture = future.whenComplete((string, ex) ->
        {
            //result.set(ex.getCause());
        }).exceptionally(ex ->
                {
                    ex.printStackTrace();
                    return "asd";
                }
        );
        TestCase.assertEquals(expectedException, result.get());
        try
        {
            final String s = completableFuture.get();
            Assert.fail("Exception should not be thrown");
        }
        catch (Exception ex)
        {
        }

    }

    @Test
    public void testCompleteExceptionaly() throws Throwable
    {
        final AtomicReference<Throwable> result = new AtomicReference<Throwable>();
        final Exception expectedException = new RuntimeException("Bla");
        final Promise<String> innerPromise = utilConcurrentCommandScheduler.supply(() ->
        {
            Thread.sleep(500);
            if (true)
            {
                throw expectedException;
            }
            return "";
        });
        final Promise<String> promise = innerPromise.whenComplete((string, ex) ->
        {
            result.set(ex);
        });
        try
        {
            waitFor(promise, 1000000);
        } catch (Exception ex)
        {
            TestCase.assertEquals(expectedException, result.get());
            TestCase.assertEquals(expectedException, ex);
        }

    }

    @Test
    public void testCompleteExceptionaly2() throws Exception
    {
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        AtomicReference<Throwable> result = new AtomicReference<Throwable>();
        Exception expectedException = new RuntimeException("Bla");
        promise.whenComplete((string, ex) ->
        {
            throw expectedException;
        }).exceptionally((ex) ->
        {
            result.set(ex);
            return null;
        });
        promise.complete("Test");
        TestCase.assertEquals(expectedException, result.get());
    }
}
