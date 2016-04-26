package org.rapla.client;

import org.junit.Test;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.rest.client.CustomConnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractProxyTest
{
    protected Map<String, String> paramMap = new LinkedHashMap<>();
    protected CustomConnector connector = createConnector();

    protected abstract CustomConnector createConnector();

    {
        paramMap.put("greeting","World");
    }

    protected abstract AnnotationProcessingTest createAnnotationProcessingProxy();

    protected abstract AnnotationSimpleProcessingTest createAnnotationSimpleProxy();

    @Test
    public void test() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        test.dontSayHello();

        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final Collection<AnnotationProcessingTest.Result> resultList = test.sayHello(p);
        final AnnotationProcessingTest.Result result = resultList.iterator().next();
        final Collection<String> ids = result.getIds();
        assertEq(2, ids.size());
        final Iterator<String> iterator = ids.iterator();
        assertEq("1", iterator.next());
        assertEq("2", iterator.next());
        test.sayHello2(p);
        test.sayHello3(p);


    }

    abstract public void assertEq(Object o1, Object o2);
    abstract public void fail_(String message);

    @Test
    public void test3() throws Exception
    {
        AnnotationSimpleProcessingTest test = createAnnotationSimpleProxy();
        final String message = "hello";
        final String result = test.sayHello(message);
        System.out.println("result");
        assertEq(Boolean.TRUE,(Boolean)result.startsWith(message));
    }

    @Test
    public void testListOfStrings() throws Exception
    {
        AnnotationSimpleProcessingTest test = createAnnotationSimpleProxy();
        final String message = "hello";
        final List<String> resultFutureResult = test.translations(message);
        assertEq(3, resultFutureResult.size());
        assertEq(message, resultFutureResult.get(0));
        assertEq(message + "_de", resultFutureResult.get(1));
        assertEq(message + "_fr", resultFutureResult.get(2));
    }

    @Test
    public void test4() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final Set<String> greeting = test.complex(paramMap).get("greeting");
        assertEq(2, greeting.size());
        final Iterator<String> iterator = greeting.iterator();
        assertEq("Hello", iterator.next());
        assertEq("World", iterator.next());
    }

    @Test
    public void testException() throws Exception
    {
        AnnotationSimpleProcessingTest test = createAnnotationSimpleProxy();
        try
        {
            final List<String> exception = test.exception();
            fail_("Exception should have been thrown");
        }
        catch (RuntimeException e)
        {

        }
    }

    @Test
    public void testCollections() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        Collection<String> primiteCollection = Arrays.asList(new String[] {"Hello", "World"});
        Collection<AnnotationProcessingTest.Parameter> complectCollection = new LinkedHashSet<>();
        complectCollection.add( new AnnotationProcessingTest.Parameter());
        final AnnotationProcessingTest.Parameter param2 = new AnnotationProcessingTest.Parameter();
        param2.setCasts( primiteCollection);
        complectCollection.add(param2);
        final String greeting = test.collecions(primiteCollection, complectCollection);
        assertEq("Made[Hello, World],[Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=null}, Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=[Hello, World]}]", greeting);
    }
}
