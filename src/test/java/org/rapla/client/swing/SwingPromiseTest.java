package org.rapla.client.swing;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.client.AbstractPromiseTest;
import org.rapla.common.ExampleService;
import org.rapla.common.ExampleService_JavaJsonProxy;
import org.rapla.logger.ConsoleLogger;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;
import org.rapla.server.ServletTestContainer;

import junit.framework.TestCase;

@Ignore
@RunWith(JUnit4.class) public class SwingPromiseTest extends AbstractPromiseTest
{

    Server server;
    Semaphore semaphore = new Semaphore(0);

    @Before @Override public void setUp() throws Exception
    {
        super.setUp();
        server = ServletTestContainer.createServer();
        server.start();
        semaphore = new Semaphore(0);
    }

    @After @Override public void tearDown() throws Exception
    {
        super.tearDown();
        ((UtilConcurrentCommandScheduler)scheduler).cancel();
        server.stop();
    }

    @Override protected void waitForTest()
    {
        try
        {
            assertEq(Boolean.TRUE, (Boolean) semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
        }
        catch (InterruptedException e)
        {
            fail_(e.getMessage());
        }
    }

    @Override protected void finishTest()
    {
        semaphore.release();
    }

    protected CustomConnector createConnector()
    {
        return new MyCustomConnector(logger,scheduler);
    }

    protected CommandScheduler createScheduler()
    {
        return new UtilConcurrentCommandScheduler(new ConsoleLogger());
    }

    protected ExampleService createAnnotationProcessingProxy()
    {
        return new ExampleService_JavaJsonProxy(connector);
    }

    @Override protected void assertEq(Object o1, Object o2)
    {
        TestCase.assertEquals(o1,o2);
    }

    @Override protected void fail_(String s)
    {
        TestCase.fail(s);
    }

}
