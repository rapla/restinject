package org.rapla.client.swing;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.common.AnnotationSimpleProcessingTest_JavaJsonProxy;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.ExceptionDeserializer;
import org.rapla.server.ServletTestContainer;

import junit.framework.TestCase;

public class MySwingTest extends TestCase
{
    Server server;

    CustomConnector connector = new MyCustomConnector();
    private Map<String, String> paramMap = new LinkedHashMap<>();
    {
        paramMap.put("greeting","World");
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        server = ServletTestContainer.createServer();
        server.start();
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        server.stop();
    }

    public void test() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        test.dontSayHello();

        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final Collection<AnnotationProcessingTest.Result> resultList = test.sayHello(p);
        final AnnotationProcessingTest.Result result = resultList.iterator().next();
        final Collection<String> ids = result.getIds();
        assertEquals(2, ids.size());
        final Iterator<String> iterator = ids.iterator();
        assertEquals("1", iterator.next());
        assertEquals("2", iterator.next());
        test.sayHello2(p);
        test.sayHello3(p);


    }

    public void test3() throws Exception
    {
        AnnotationSimpleProcessingTest test = new AnnotationSimpleProcessingTest_JavaJsonProxy(connector);
        final String message = "hello";
        final String result = test.sayHello(message);
        System.out.println("result");
        assertTrue(result.startsWith(message));
    }

    public void testListOfStrings() throws Exception
    {
        AnnotationSimpleProcessingTest test = new AnnotationSimpleProcessingTest_JavaJsonProxy(connector);
        final String message = "hello";
        final List<String> resultFutureResult = test.translations(message);
        assertEquals(3, resultFutureResult.size());
        assertEquals(message, resultFutureResult.get(0));
        assertEquals(message + "_de", resultFutureResult.get(1));
        assertEquals(message + "_fr", resultFutureResult.get(2));
    }

    public void test4() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        final Set<String> greeting = test.complex(paramMap).get("greeting");
        assertEquals(2, greeting.size());
        final Iterator<String> iterator = greeting.iterator();
        assertEquals("Hello", iterator.next());
        assertEquals("World", iterator.next());
    }

    public void testException() throws Exception
    {
        AnnotationSimpleProcessingTest test = new AnnotationSimpleProcessingTest_JavaJsonProxy(connector);
        try
        {
            final List<String> exception = test.exception();
            Assert.fail("Exception should have been thrown");
        }
        catch (RuntimeException e)
        {

        }
    }

    public void testCollections() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        Collection<String> primiteCollection = Arrays.asList(new String[] {"Hello", "World"});
        Collection<AnnotationProcessingTest.Parameter> complectCollection = new LinkedHashSet<>();
        complectCollection.add( new AnnotationProcessingTest.Parameter());
        final AnnotationProcessingTest.Parameter param2 = new AnnotationProcessingTest.Parameter();
        param2.setCasts( primiteCollection);
        complectCollection.add(param2);
        final String greeting = test.collecions(primiteCollection, complectCollection);
        assertEquals("Made[Hello, World],[Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=null}, Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=[Hello, World]}]", greeting);
    }

}
