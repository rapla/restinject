package org.rapla.client.swing;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.rest.client.EntryPointFactory;
import org.rapla.rest.client.swing.BasicRaplaHTTPConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.impl.UtilConcurrentCommandScheduler;
import org.rapla.server.ServletTestContainer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SwingPromiseTest extends TestCase
{

    Server server;

    BasicRaplaHTTPConnector.CustomConnector connector = new MyCustomConnector();
    CommandScheduler scheduler;


    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        scheduler = new UtilConcurrentCommandScheduler()
        {
            @Override protected void error(String message, Exception ex)
            {
                System.err.println( message);
            }

            @Override protected void debug(String message)
            {
                System.out.println( message);
            }

            @Override protected void info(String message)
            {
                System.out.println( message);
            }

            @Override protected void warn(String message)
            {
                System.err.println( message);
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

    public void test() throws Exception
    {
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(connector);
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        Semaphore semaphore = new Semaphore(0);
        final Promise<List<AnnotationProcessingTest.Result>> supply = scheduler.supply(() -> test.sayHello(p));
        supply.thenAccept( (resultList) -> {
                    final AnnotationProcessingTest.Result result = resultList.get(0);
                    final List<String> ids = result.getIds();
                    assertEquals(2, ids.size());
                    assertEquals("1", ids.get(0));
                    assertEquals("2", ids.get(1));
                    semaphore.release();
                });
        assertTrue(semaphore.tryAcquire(10000,TimeUnit.MILLISECONDS));
    }
}
