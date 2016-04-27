package org.rapla.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractPromiseTest
{

    protected CustomConnector connector;
    protected CommandScheduler scheduler;
    private Map<String, String> paramMap = new LinkedHashMap<>();

    @Before public void setUp() throws Exception
    {
        paramMap.put("greeting", "World");
        scheduler = createScheduler();
        connector = createConnector();
    }

    protected abstract CommandScheduler createScheduler();

    protected abstract CustomConnector createConnector();

    protected abstract AnnotationProcessingTest createAnnotationProcessingProxy();

    protected abstract void assertEq(Object o1, Object o2);

    protected abstract void fail_(String s);

    protected abstract void waitForTest();

    protected abstract void finishTest();

    @After public void tearDown() throws Exception
    {
    }

    @Test public void testAccept() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final Promise<Collection<AnnotationProcessingTest.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.thenAccept((resultList) -> {
            final AnnotationProcessingTest.Result result = resultList.iterator().next();
            final Collection<String> ids = result.getIds();
            assertEq(2, ids.size());
            final Iterator<String> iterator = ids.iterator();
            assertEq("1", iterator.next());
            assertEq("2", iterator.next());
            finishTest();
        });
        waitForTest();
    }

    @Test public void testHandle1() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final AnnotationProcessingTest.Parameter p = null;
        final Promise<Collection<AnnotationProcessingTest.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.handle((resultList, ex) -> {
            if (ex != null)
            {
                finishTest();
            }
            else
            {
                fail_("Exception should have occured");
            }
            return null;
        });
        waitForTest();
    }

    @Test public void testHandle2() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        AnnotationProcessingTest.Parameter p2 = new AnnotationProcessingTest.Parameter();
        p2.setActionIds(Arrays.asList(new Integer[] { 3, 5 }));
        final Promise<Collection<AnnotationProcessingTest.Result>> successPromise = scheduler.supply(() -> test.sayHello(p2));
        successPromise.handle((resultList, ex) -> {
            if (ex != null)
            {
                fail_("No exception should have occured");
            }
            else
            {
                finishTest();
            }
            return null;
        });
        waitForTest();
    }

    @Test public void testApplyAccept() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final Promise<Map<String, Set<String>>> supply = scheduler.supply(() -> test.complex(paramMap));
        supply.thenApply((map) -> {
            return map.keySet();
        }).thenAccept((s) -> {
            System.out.println("got keys: " + s);
            finishTest();
        });
        waitForTest();
    }

    @Test public void testApplyRun() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final AtomicReference<Map<String, Set<String>>> result = new AtomicReference<Map<String, Set<String>>>(null);
        final Promise<Map<String, Set<String>>> supply = scheduler.supply(() -> test.complex(paramMap));
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
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        AtomicReference<Map<String, Set<String>>> result = new AtomicReference<Map<String, Set<String>>>(null);
        final Promise<Map<String, Set<String>>> promise1 = scheduler.supplyProxy(() -> test.complex(paramMap));
        final Promise<Map<String, Set<String>>> promise2 = scheduler.supplyProxy(() -> test.complex(paramMap));
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
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final Promise<Map<String, Set<String>>> promise = scheduler.supplyProxy(() -> test.complex(paramMap));
        final AtomicReference<Map> result1 = new AtomicReference<Map>();
        promise.thenCompose((map) -> {
            AnnotationProcessingTest.Parameter param = new AnnotationProcessingTest.Parameter();
            param.setActionIds(Arrays.asList(new Integer[] { map.keySet().size(), map.values().size() }));
            result1.set(map);
            return scheduler.supplyProxy(() -> test.sayHello(param)).thenAccept((list) -> {
                final AnnotationProcessingTest.Result resultParam = list.iterator().next();
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
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final Promise<Collection<AnnotationProcessingTest.Result>> promise = scheduler.supplyProxy(() -> test.sayHello(null));
        promise.exceptionally((ex) -> {
            finishTest();
            return null;
        });
        waitForTest();
    }

    @Test public void testApplyToEither() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final Promise<String> promise = scheduler.supplyProxy(() -> test.longcall());
        final Promise<String> promise2 = scheduler.supplyProxy(() -> test.shortcall());
        promise.applyToEither(promise2, (first) -> {
            assertEq("short",  first);
            finishTest();
            return null;
        });
        waitForTest();
    }
}
