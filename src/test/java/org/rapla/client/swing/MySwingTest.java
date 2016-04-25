package org.rapla.client.swing;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
        final List<AnnotationProcessingTest.Result> resultList = test.sayHello(p);
        final AnnotationProcessingTest.Result result = resultList.get(0);
        final List<String> ids = result.getIds();
        assertEquals(2, ids.size());
        assertEquals("1", ids.get(0));
        assertEquals("2", ids.get(1));
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

    public void test5() throws Exception
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

}
