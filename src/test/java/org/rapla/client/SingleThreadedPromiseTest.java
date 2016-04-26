package org.rapla.client;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.client.SingleThreadedPromise;
import org.rapla.scheduler.impl.UtilConcurrentCommandScheduler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class) public class SingleThreadedPromiseTest
{
    @Test public void testAccept1() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();

        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });

        AtomicReference<String> acceptedText2 = new AtomicReference<String>();
        promise.thenAccept((t) -> {
            acceptedText2.set(t);
            semaphore.release();
        });
        promise.complete(text);
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
        TestCase.assertEquals(text, acceptedText2.get());
    }

    @Test public void testAccept1CompletableFuture() throws Exception
    {
        String text = "Hello World";
        final UtilConcurrentCommandScheduler utilConcurrentCommandScheduler = new UtilConcurrentCommandScheduler()
        {
            @Override protected void error(String message, Exception ex)
            {

            }

            @Override protected void debug(String message)
            {

            }

            @Override protected void info(String message)
            {

            }

            @Override protected void warn(String message)
            {

            }
        };
        Promise<String> promise = utilConcurrentCommandScheduler.supply(() -> {
            Thread.sleep(500);
            return text;
        });
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });

        AtomicReference<String> acceptedText2 = new AtomicReference<String>();
        promise.thenAccept((t) -> {
            acceptedText2.set(t);
            semaphore.release();
        });
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
        TestCase.assertEquals(text, acceptedText2.get());
    }

    @Test public void testAccept2() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.complete(text);
        promise.thenAccept((t) -> {
            acceptedText.set(t);
        });
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testApply1() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenApply((t) -> {
            return t.toLowerCase();
        }).thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });
        promise.complete(text);
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text.toLowerCase(), acceptedText.get());
    }

    @Test public void testApply2() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.complete(text);
        promise.thenApply((t) -> t.toLowerCase()).thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text.toLowerCase(), acceptedText.get());
    }

    @Test public void testExceptionaly1() throws Exception
    {
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        String text = "Fehler";
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<Throwable> acceptedText = new AtomicReference<Throwable>();
        promise.exceptionally((ex) -> {
            acceptedText.set(ex);
            semaphore.release();
            return null;
        });
        promise.abort(new RuntimeException(text));
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get().getMessage());
    }

    @Test public void testExceptionaly2() throws Exception
    {
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        String text = "Fehler";
        AtomicReference<Throwable> acceptedText = new AtomicReference<Throwable>();
        Semaphore semaphore = new Semaphore(0);
        promise.abort(new RuntimeException(text));
        promise.exceptionally((ex) -> {
            acceptedText.set(ex);
            semaphore.release();
            return null;
        });
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get().getMessage());
    }

    @Test public void testExceptionaly3() throws Exception
    {
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        String text = "Fallback";
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        Semaphore semaphore = new Semaphore(0);
        promise.abort(new RuntimeException(text));
        promise.exceptionally((ex) -> text).thenAccept((fallback) -> {
            acceptedText.set(fallback);
            semaphore.release();
        });
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testCombine1() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        SingleThreadedPromise<String> promise2 = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenCombine(promise2, (t1, t2) -> t1 +" "+ t2).thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });
        promise.complete("Hello");
        promise2.complete("World");
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testCombine2() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        SingleThreadedPromise<String> promise2 = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenCombine(promise2, (t1, t2) -> t1 +" "+ t2).thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });
        promise2.complete("World");
        promise.complete("Hello");
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testCombine3() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        SingleThreadedPromise<String> promise2 = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise2.complete("World");
        promise.complete("Hello");
        promise.thenCombine(promise2, (t1, t2) -> t1 +" "+ t2).thenAccept((t) -> {
            acceptedText.set(t);
            semaphore.release();
        });

        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testCombineWithException1() throws Exception
    {
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        SingleThreadedPromise<String> promise2 = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.complete("Hello");
        AtomicReference<Throwable> acceptedException = new AtomicReference<Throwable>();
        promise2.abort(new RuntimeException("gone"));
        promise.thenCombine(promise2, (t1, t2) -> t1 +" "+ t2).thenAccept((t) -> {
            acceptedText.set(t);
        }).exceptionally((ex)-> {acceptedException.set( ex);semaphore.release();return null;});
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertNull(acceptedText.get());
        TestCase.assertEquals("gone", acceptedException.get().getMessage());
    }

    @Test public void testCombineWithException2() throws Exception
    {
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();
        SingleThreadedPromise<String> promise2 = new SingleThreadedPromise<String>();
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise2.complete("Hello");
        AtomicReference<Throwable> acceptedException = new AtomicReference<Throwable>();
        promise.abort(new RuntimeException("gone"));
        promise.thenCombine(promise2, (t1, t2) -> t1 +" "+ t2).thenAccept((t) -> {
            acceptedText.set(t);
        }).exceptionally((ex)-> {acceptedException.set( ex);semaphore.release();return null;});
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertNull(acceptedText.get());
        TestCase.assertEquals("gone", acceptedException.get().getMessage());
    }

    @Test public void testCompose() throws Exception
    {
        String text = "Hello World";
        SingleThreadedPromise<String> promise = new SingleThreadedPromise<String>();

        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenCompose((t)-> {
            SingleThreadedPromise<String> promise2 = new SingleThreadedPromise<String>();
            promise2.complete(t + " World");
            semaphore.release();
            return promise2;
        }).thenAccept((t)->
        {
            acceptedText.set(t);
        }
        );
        promise.complete("Hello");
        TestCase.assertTrue(semaphore.tryAcquire(1000, TimeUnit.MILLISECONDS));
        TestCase.assertEquals(text, acceptedText.get());
    }

}
