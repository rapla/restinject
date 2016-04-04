package org.rapla.client.swing;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.common.AnnotationSimpleProcessingTest_JavaJsonProxy;
import org.rapla.jsonrpc.client.EntryPointFactory;
import org.rapla.jsonrpc.client.gwt.MockProxy;
import org.rapla.jsonrpc.client.swing.BasicRaplaHTTPConnector;
import org.rapla.jsonrpc.client.swing.RaplaConnectException;
import org.rapla.server.ServletTestContainer;

import junit.framework.TestCase;

public class MySwingTest extends TestCase
{
    Server server;

    BasicRaplaHTTPConnector.CustomConnector connector = new BasicRaplaHTTPConnector.CustomConnector()
    {

        String accessToken;
        Executor executor = new Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                command.run();
            }
        };

        @Override
        public String reauth(BasicRaplaHTTPConnector proxy) throws Exception
        {
            return accessToken;
        }

        @Override
        public String getAccessToken()
        {
            return accessToken;
        }

        @Override
        public Exception deserializeException(String classname, String s, List<String> params)
        {
            return new Exception(classname + " " + s + " " + params);
            // throw new Au
        }

        @Override
        public Class[] getNonPrimitiveClasses()
        {
            return new Class[0];
        }

        @Override
        public Exception getConnectError(IOException ex)
        {
            return new RaplaConnectException("Connection Error " + ex.getMessage());
        }

        @Override
        public Executor getScheduler()
        {
            return executor;
        }

        @Override
        public MockProxy getMockProxy()
        {
            return null;
        }
    };

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
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

    public void test() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
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
        final Set<String> greeting = test.complex().get("greeting");
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
