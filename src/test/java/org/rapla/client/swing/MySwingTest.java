package org.rapla.client.swing;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.common.AnnotationSimpleProcessingTest_JavaJsonProxy;
import org.rapla.jsonrpc.client.EntryPointFactory;
import org.rapla.jsonrpc.client.gwt.MockProxy;
import org.rapla.jsonrpc.client.swing.BasicRaplaHTTPConnector;
import org.rapla.jsonrpc.client.swing.RaplaConnectException;
import org.rapla.jsonrpc.common.FutureResult;
import org.rapla.server.ServletTestContainer;
import org.rapla.server.TestServlet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;

public class MySwingTest extends TestCase
{
    Server server;

    BasicRaplaHTTPConnector.CustomConnector connector = new BasicRaplaHTTPConnector.CustomConnector()
    {

        String accessToken;
        Executor executor = new Executor()
        {
            @Override public void execute(Runnable command)
            {
                command.run();
            }
        };

        @Override public String reauth(BasicRaplaHTTPConnector proxy) throws Exception
        {
            return accessToken;
        }

        @Override public String getAccessToken()
        {
            return accessToken;
        }

        @Override public Exception deserializeException(String classname, String s, List<String> params)
        {
            return new Exception(classname + " " + s + " " + params);
            // throw new Au
        }

        @Override public Class[] getNonPrimitiveClasses()
        {
            return new Class[0];
        }

        @Override public Exception getConnectError(IOException ex)
        {
            return new RaplaConnectException("Connection Error " + ex.getMessage());
        }

        @Override public Executor getScheduler()
        {
            return executor;
        }

        @Override public MockProxy getMockProxy()
        {
            return null;
        }
    };


    @Override protected void setUp() throws Exception
    {
        super.setUp();
        server = ServletTestContainer.createServer(TestServlet.class);
        BasicRaplaHTTPConnector.setServiceEntryPointFactory(new EntryPointFactory()
        {
            @Override public String getEntryPoint(String interfaceName, String relativePath)
            {
                return "http://localhost:8052/" + "rapla/" + (relativePath != null ? relativePath : interfaceName);
            }
        });
    }

    @Override protected void tearDown() throws Exception
    {
        super.tearDown();
        server.stop();
    }

    public void test() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy( connector );
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final FutureResult<List<AnnotationProcessingTest.Result>> resultFutureResult = test.sayHello(p);
        final List<AnnotationProcessingTest.Result> resultList = resultFutureResult.get();
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
        AnnotationSimpleProcessingTest test = new AnnotationSimpleProcessingTest_JavaJsonProxy( connector );
        final String message = "hello";
        final FutureResult<String> resultFutureResult = test.sayHello(message);
        final String result = resultFutureResult.get();
        System.out.println("result");
        assertTrue(result.startsWith(message));
    }

    public void test4() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy( connector );
        final FutureResult<Map<String,Set<String>>> resultFutureResult = test.complex();
        final StringBuilder builder = new StringBuilder();
        final Set<String> greeting = resultFutureResult.get().get("greeting");
        assertEquals(2,greeting.size());
        final Iterator<String> iterator = greeting.iterator();
        assertEquals("Hello",iterator.next());
        assertEquals("World", iterator.next());
    }
}
