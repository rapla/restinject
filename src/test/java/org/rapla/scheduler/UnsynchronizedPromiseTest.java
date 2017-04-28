package org.rapla.scheduler;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.ConsoleLogger;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class) public class UnsynchronizedPromiseTest
{
    @Test public void testAccept1() throws Exception
    {
        String text = "Hello World";
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();

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



    @Test public void testAccept2() throws Exception
    {
        String text = "Hello World";
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.complete(text);
        promise.thenAccept((t) -> {
            acceptedText.set(t);
        });
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testComplete() throws Exception
    {
        String text = "Hello World";
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.whenComplete((string,ex) -> {
            acceptedText.set( string);
        });
        promise.complete(text);
        TestCase.assertEquals(text, acceptedText.get());
    }

    @Test public void testCompleteExceptionaly() throws Exception
    {
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        AtomicReference<Throwable> result = new AtomicReference<Throwable>();
        Exception expectedException = new RuntimeException("Bla");
        promise.whenComplete((string,ex) -> {
            result.set( ex);
        });
        promise.abort(expectedException);
        TestCase.assertEquals(expectedException, result.get());
    }

    @Test
    public void testCompleteExceptionaly2() throws Exception
    {
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        AtomicReference<Throwable> result = new AtomicReference<Throwable>();
        Exception expectedException = new RuntimeException("Bla");
        promise.whenComplete((string,ex) -> {
            throw expectedException;
        }).exceptionally((ex)->
        {
            result.set( ex);
            return null;
        }
        );
        promise.complete("Test");
        TestCase.assertEquals(expectedException, result.get());
    }

    @Test public void testApply1() throws Exception
    {
        String text = "Hello World";
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        UnsynchronizedPromise<String> promise2 = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        UnsynchronizedPromise<String> promise2 = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        UnsynchronizedPromise<String> promise2 = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        UnsynchronizedPromise<String> promise2 = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();
        UnsynchronizedPromise<String> promise2 = new UnsynchronizedPromise<String>();
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
        UnsynchronizedPromise<String> promise = new UnsynchronizedPromise<String>();

        Semaphore semaphore = new Semaphore(0);
        AtomicReference<String> acceptedText = new AtomicReference<String>();
        promise.thenCompose((t)-> {
            UnsynchronizedPromise<String> promise2 = new UnsynchronizedPromise<String>();
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
