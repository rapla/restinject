package org.rapla.client.swing;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.client.AbstractProxyTest;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.common.AnnotationSimpleProcessingTest_JavaJsonProxy;
import org.rapla.rest.client.CustomConnector;
import org.rapla.server.ServletTestContainer;

@RunWith(JUnit4.class)
public class MySwingTest extends AbstractProxyTest
{
    Server server;

    @Override protected CustomConnector createConnector()
    {
        return new MyCustomConnector();
    }
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        server = ServletTestContainer.createServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception
    {
        server.stop();
    }

    protected AnnotationProcessingTest_JavaJsonProxy createAnnotationProcessingProxy()
    {
        return new AnnotationProcessingTest_JavaJsonProxy(connector);
    }

    protected AnnotationSimpleProcessingTest_JavaJsonProxy createAnnotationSimpleProxy()
    {
        return new AnnotationSimpleProcessingTest_JavaJsonProxy(connector);
    }

    @Override public void assertEq(Object o1, Object o2)
    {
        TestCase.assertEquals(o1,o2);
    }

    @Override public void fail_(String message)
    {
        TestCase.fail(message);
    }

}
