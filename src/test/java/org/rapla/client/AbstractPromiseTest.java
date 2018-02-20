package org.rapla.client;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rapla.common.ExampleService;
import org.rapla.logger.ConsoleLogger;
import org.rapla.logger.Logger;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractPromiseTest
{

    protected CustomConnector connector;
    protected CommandScheduler scheduler;
    protected Logger logger;
    private Map<String, String> paramMap = new LinkedHashMap<>();

    @Before public void setUp() throws Exception
    {
        paramMap.put("greeting", "World");
        scheduler = createScheduler();
        connector = createConnector();
        logger = new ConsoleLogger();
    }

    protected abstract CommandScheduler createScheduler();

    protected abstract CustomConnector createConnector();

    protected abstract ExampleService createAnnotationProcessingProxy();

    protected abstract void assertEq(Object o1, Object o2);

    protected abstract void fail_(String s);

    protected abstract void waitForTest();

    protected abstract void finishTest();

    @After public void tearDown() throws Exception
    {
    }

    @Test public void testCatch()
    {
        Promise<String> async4 = scheduler.supply(() -> {return 0/0+"Hello";});
        async4.thenAccept((string)->System.out.println(string)).exceptionally( (ex)->ex.printStackTrace());
        try
        {
            String asd;
            asd = 0 / 0 + "Hello";
            System.out.println("All done: " + asd);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.out.println("All done.");

    }

    @Test public void testAccept()
    {
        ExampleService test = createAnnotationProcessingProxy();
        ExampleService.Parameter p = new ExampleService.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final Promise<Collection<ExampleService.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.thenAccept((resultList) -> {
            final ExampleService.Result result = resultList.iterator().next();
            final Collection<String> ids = result.getIds();
            assertEq(2, ids.size());
            final Iterator<String> iterator = ids.iterator();
            assertEq("1", iterator.next());
            assertEq("2", iterator.next());
            finishTest();
        });
        waitForTest();
    }

    @Test public void testHandle1()
    {
        ExampleService test = createAnnotationProcessingProxy();
        final ExampleService.Parameter p = null;
        final Promise<Collection<ExampleService.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.handle((resultList, ex) -> {
            if (ex != null)
            {
                finishTest();
            }
            else
            {
                fail_("Exception should have occured");
            }
            return resultList;
        });
        waitForTest();
    }

    @Test public void testHandle2() throws Exception
    {
        ExampleService test = createAnnotationProcessingProxy();
        ExampleService.Parameter p2 = new ExampleService.Parameter();
        p2.setActionIds(Arrays.asList(new Integer[] { 3, 5 }));
        final Promise<Collection<ExampleService.Result>> successPromise = scheduler.supply(() -> test.sayHello(p2));
        successPromise.handle((resultList, ex) -> {
            if (ex != null)
            {
                fail_("No exception should have occured");
            }
            else
            {
                finishTest();
            }
            return resultList;
        });
        waitForTest();
    }

    @Test public void testApplyAccept() throws Exception
    {
        ExampleService test = createAnnotationProcessingProxy();
        final Promise<Map<String, Set<String>>> supply = test.complex(paramMap);
        supply.thenApply((map) -> map.keySet()).thenAccept((s) -> {
            System.out.println("got keys: " + s);
            finishTest();
        });
        waitForTest();
    }

    private static class RefContainer<T>
    {
        T t;

        public RefContainer()
        {
            this(null);
        }
        public RefContainer(T t)
        {
            this.t = t;
        }

        public T get()
        {
            return t;
        }

        public void set(T t)
        {
            this.t = t;
        }
    }

    @Test public void testApplyRun() throws Exception
    {
        ExampleService test = createAnnotationProcessingProxy();
        final RefContainer<Map<String, Set<String>>> result = new RefContainer<Map<String, Set<String>>>(null);
        final Promise<Map<String, Set<String>>> supply = test.complex(paramMap);
        supply.thenApply((map) -> {
            // assume setting in data model
            result.set(map);
            return map;
        }).thenRun(() -> {
            // Check data model
            final Map<String, Set<String>> map = result.get();
            assertEq(Boolean.TRUE,(Boolean)(map != null));
            System.out.println("got keys: " + map.keySet());
            finishTest();
        });
        waitForTest();
    }

    @Test public void testCombine() throws Exception
    {
        ExampleService test = createAnnotationProcessingProxy();
        final Promise<Map<String, Set<String>>> promise1 = test.complex(paramMap);
        final Promise<Map<String, Set<String>>> promise2 = test.complex(paramMap);
        promise1.thenCombine(promise2, (map1, map2) -> {
            final Set<String> keySet1 = map1.keySet();
            final Set<String> keySet2 = map2.keySet();
            assertEq(Boolean.TRUE,(Boolean)keySet1.containsAll(keySet2));
            assertEq(Boolean.TRUE,(Boolean)keySet2.containsAll(keySet1));
            finishTest();
            return map1;
        });
        waitForTest();
    }

    @Test public void testCompose() throws Exception
    {
        ExampleService test = createAnnotationProcessingProxy();
        final Promise<Map<String, Set<String>>> promise = test.complex(paramMap);
        final RefContainer<Map> result1 = new RefContainer<Map>();
        promise.thenCompose((map) -> {
            ExampleService.Parameter param = new ExampleService.Parameter();
            param.setActionIds(Arrays.asList(new Integer[] { map.keySet().size(), map.values().size() }));
            result1.set(map);
            return scheduler.supply(() -> test.sayHello(param)).thenAccept((list) -> {
                final ExampleService.Result resultParam = list.iterator().next();
                final Collection<String> ids = resultParam.getIds();
                final Map internalMap = result1.get();
                final Iterator<String> iterator = ids.iterator();
                assertEq(internalMap.keySet().size() + "", iterator.next());
                assertEq(internalMap.values().size() + "", iterator.next());
                finishTest();
            });
        });
        waitForTest();
    }

    @Test public void testExceptionally() throws Exception
    {
        ExampleService test = createAnnotationProcessingProxy();
        final Promise<Map<String, Set<String>>> promise = test.complex(null);
        promise.exceptionally((ex) -> {
            assertEq(NullPointerException.class,ex.getClass());
            logger.error("Client Nullpointer expected", ex);
            finishTest();
        });
        waitForTest();
    }

}
