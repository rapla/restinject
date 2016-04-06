package org.rapla.client.swing;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest.Parameter;
import org.rapla.common.AnnotationProcessingTest.Result;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.rest.client.EntryPointFactory;
import org.rapla.rest.client.swing.BasicRaplaHTTPConnector;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.impl.UtilConcurrentCommandScheduler;
import org.rapla.server.ServletTestContainer;

import junit.framework.TestCase;

public class SwingPromiseTest extends TestCase
{

    Server server;

    BasicRaplaHTTPConnector.CustomConnector connector = new MyCustomConnector();
    UtilConcurrentCommandScheduler scheduler;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        scheduler = new UtilConcurrentCommandScheduler()
        {
            @Override
            protected void error(String message, Exception ex)
            {
                System.err.println(message);
            }

            @Override
            protected void debug(String message)
            {
                System.out.println(message);
            }

            @Override
            protected void info(String message)
            {
                System.out.println(message);
            }

            @Override
            protected void warn(String message)
            {
                System.err.println(message);
            }

        };
        server = ServletTestContainer.createServer();
        server.start();
        BasicRaplaHTTPConnector.setServiceEntryPointFactory(new EntryPointFactory()
        {
            @Override
            public String getEntryPoint(String interfaceName, String relativePath)
            {
                return "http://localhost:8052/" + "rest/" + (relativePath != null ? relativePath : interfaceName);
            }
        });
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        server.stop();
    }

    public void testAccept() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        Semaphore semaphore = new Semaphore(0);
        final Promise<List<AnnotationProcessingTest.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.thenAccept((resultList) ->
        {
            final AnnotationProcessingTest.Result result = resultList.get(0);
            final List<String> ids = result.getIds();
            assertEquals(2, ids.size());
            assertEquals("1", ids.get(0));
            assertEquals("2", ids.get(1));
            semaphore.release();
        });
        assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
    }

    public void testHandle() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        final AnnotationProcessingTest.Parameter p = null;
        final Promise<List<AnnotationProcessingTest.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.handle((resultList, ex) ->
        {
            if (ex != null)
            {
                semaphore.release();
            }
            else
            {
                fail("Exception should have occured");
            }
            return null;
        });
        assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
        AnnotationProcessingTest.Parameter p2 = new AnnotationProcessingTest.Parameter();
        p2.setActionIds(Arrays.asList(new Integer[] { 3, 5 }));
        final Promise<List<Result>> successPromise = scheduler.supply(() -> test.sayHello(p2));
        successPromise.handle((resultList, ex) ->
        {
            if (ex != null)
            {
                fail("No exception should have occured");
            }
            else
            {
                semaphore.release();
            }
            return null;
        });
        assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
    }

    public void testApplyAccept() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        final Promise<Map<String, Set<String>>> supply = scheduler.supply(() -> test.complex());
        supply.thenApply((map) ->
        {
            return map.keySet();
        }).thenAccept((s) ->
        {
            System.out.println("got keys: " + s);
            semaphore.release();
        });
        Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testApplyRun() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        final AtomicReference<Map<String, Set<String>>> result = new AtomicReference<Map<String, Set<String>>>(null);
        final Promise<Map<String, Set<String>>> supply = scheduler.supply(() -> test.complex());
        supply.thenApply((map) ->
        {
            // assume setting in data model
            result.set(map);
            return map;
        }).thenRun(() ->
        {
            semaphore.release();
        });
        Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));
        // Check data model
        final Map<String, Set<String>> map = result.get();
        Assert.assertNotNull(map);
        System.out.println("got keys: " + map.keySet());
    }

    public void testCombine() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        AtomicReference<Map<String, Set<String>>> result = new AtomicReference<Map<String, Set<String>>>(null);
        final Promise<Map<String, Set<String>>> promise1 = scheduler.supplyProxy(() -> test.complex());
        final Promise<Map<String, Set<String>>> promise2 = scheduler.supplyProxy(() -> test.complex());
        promise1.thenCombine(promise2, (map1, map2) ->
        {
            final Set<String> keySet1 = map1.keySet();
            final Set<String> keySet2 = map2.keySet();
            Assert.assertTrue(keySet1.containsAll(keySet2));
            Assert.assertTrue(keySet2.containsAll(keySet1));
            semaphore.release();
            return map1;
        });
        Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));
    }

    public void testCompose() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        final Promise<Map<String, Set<String>>> promise = scheduler.supplyProxy(() -> test.complex());
        final AtomicReference<Map> result1 = new AtomicReference<Map>();
        promise.thenCompose((map) ->
        {
            Parameter param = new AnnotationProcessingTest.Parameter();
            param.setActionIds(Arrays.asList(new Integer[] { map.keySet().size(), map.values().size() }));
            result1.set(map);
            return scheduler.supplyProxy(() -> test.sayHello(param)).thenAccept((list) ->
            {
                final Result resultParam = list.get(0);
                final List<String> ids = resultParam.getIds();
                final Map internalMap = result1.get();
                Assert.assertEquals(internalMap.keySet().size() + "", ids.get(0));
                Assert.assertEquals(internalMap.values().size() + "", ids.get(1));
                semaphore.release();
            });
        });
        Assert.assertTrue(semaphore.tryAcquire(20, TimeUnit.SECONDS));
    }

    public void testExceptionally() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        final Promise<List<AnnotationProcessingTest.Result>> promise = scheduler.supplyProxy(() -> test.sayHello(null));
        promise.exceptionally((ex) ->
        {
            semaphore.release();
            return null;
        });
        Assert.assertTrue(semaphore.tryAcquire(20, TimeUnit.SECONDS));
    }

    public void test() throws Exception
    {
        final Semaphore semaphore = new Semaphore(0);
        final Promise<Integer> promise = scheduler.supply(() ->
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                fail(e.getMessage());
            }
            return 1000;
        });
        final Promise<Integer> promise2 = scheduler.supply(() ->
        {
            return 100;
        });
        promise.applyToEither(promise2, (first) ->
        {
            Assert.assertEquals(100, (int) first);
            semaphore.release();
            return null;
        });
        Assert.assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));
    }
}
