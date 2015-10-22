package org.rapla.client.swing;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest;
import org.rapla.gwtjsonrpc.client.impl.EntryPointFactory;
import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.rest.client.BasicRaplaHTTPConnector;
import org.rapla.server.ServletTestContainer;
import org.rapla.server.TestServlet;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

public class MySwingTest extends TestCase
{
    Server server;

    String message = "ConnectionError";
    Executor executor = new Executor()
    {
        @Override public void execute(Runnable command)
        {
            command.run();
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
                return "http://localhost:8052/"+ "rapla/json/" + (relativePath != null ? relativePath : interfaceName);
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
        AnnotationProcessingTest test = new org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest_JavaJsonProxy( executor, message);
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final FutureResult<AnnotationProcessingTest.Result> resultFutureResult = test.sayHello(p);
        final AnnotationProcessingTest.Result result = resultFutureResult.get();
        final List<String> ids = result.getIds();
        assertEquals(2, ids.size());
        assertEquals("1", ids.get(0));
        assertEquals("2", ids.get(1));
        test.sayHello2(p);
    }
}
