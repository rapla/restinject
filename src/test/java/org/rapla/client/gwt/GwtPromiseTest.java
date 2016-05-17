package org.rapla.client.gwt;

import com.google.gwt.junit.client.GWTTestCase;
import org.junit.Test;
import org.rapla.client.AbstractPromiseTest;
import org.rapla.common.ExampleService;
import org.rapla.common.ExampleService_GwtJsonProxy;
import org.rapla.logger.ConsoleLogger;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.client.gwt.GwtCommandScheduler;

public class GwtPromiseTest extends GWTTestCase
{

    AbstractPromiseTest genericTest = new AbstractPromiseTest()
    {
        @Override protected CommandScheduler createScheduler()
        {
            CommandScheduler scheduler = new GwtCommandScheduler(new ConsoleLogger())
            {
                @Override protected void warn(String message, Exception e)
                {
                    e.printStackTrace(System.err);
                    System.err.println(message);
                }

            };
            return scheduler;
        }

        @Override protected CustomConnector createConnector()
        {
            return new GwtCustomConnector();
        }

        @Override protected ExampleService createAnnotationProcessingProxy()
        {
            return new ExampleService_GwtJsonProxy(connector);
        }

        @Override public void assertEq(Object o1, Object o2)
        {
            GWTTestCase.assertEquals(o1, o2);
        }

        @Override public void fail_(String message)
        {
            GWTTestCase.fail(message);
        }

        @Override protected void waitForTest()
        {
            GwtPromiseTest.this.delayTestFinish(10000);
        }

        @Override protected void finishTest()
        {
            GwtPromiseTest.this.finishTest();
        }
    };

    /**
     * Specifies a module to use when running this test case. The returned
     * module must include the source for this class.
     *
     * @see GWTTestCase#getModuleName()
     */
    @Override public String getModuleName()
    {
        return "org.rapla.GwtTest";
    }

    @Override protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        genericTest.setUp();
    }

    @Override protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();
        genericTest.tearDown();
    }

    @Test public void testCompose() throws Exception
    {
        genericTest.testCompose();
    }

    @Test public void testCombine() throws Exception
    {
        genericTest.testCombine();
    }

    @Test public void testApplyAccept() throws Exception
    {
        genericTest.testApplyAccept();
    }

    @Test public void testApplyRun() throws Exception
    {
        genericTest.testApplyRun();
    }

    @Test public void testHandle1() throws Exception
    {
        genericTest.testHandle1();
    }

    @Test public void testHandle2() throws Exception
    {
        genericTest.testHandle2();
    }


    @Test public void testExceptionally() throws Exception
    {
        genericTest.testExceptionally();
    }

    @Test public void testApplyToEither() throws Exception
    {
        genericTest.testApplyToEither();
    }

    @Test public void testAccept() throws Exception
    {
        genericTest.testAccept();
    }
}