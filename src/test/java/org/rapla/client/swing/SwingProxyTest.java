package org.rapla.client.swing;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.client.AbstractProxyTest;
import org.rapla.common.ExampleService_JavaJsonProxy;
import org.rapla.common.ExampleSimpleService_JavaJsonProxy;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.swing.HTTPConnector;
import org.rapla.rest.client.swing.JsonRemoteConnector;
import org.rapla.server.ServletTestContainer;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

@RunWith(JUnit4.class)
public class SwingProxyTest extends AbstractProxyTest
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

    protected ExampleService_JavaJsonProxy createExampleServiceProxy()
    {
        return new ExampleService_JavaJsonProxy(connector);
    }

    protected ExampleSimpleService_JavaJsonProxy createExampleSimpleServiceProxy()
    {
        return new ExampleSimpleService_JavaJsonProxy(connector);
    }

    @Override public void assertEq(Object o1, Object o2)
    {
        TestCase.assertEquals(o1,o2);
    }

    @Override public void fail_(String message)
    {
        TestCase.fail(message);
    }

    @Test
    public void testOptions() throws IOException
    {
        HTTPConnector connector = new HTTPConnector();
        final URL url = new URL(this.connector.getFullQualifiedUrl("ExampleSimpleService"));
        String token =null;
        String body = null;
        final JsonRemoteConnector.CallResult options = connector.sendCallWithString("OPTIONS", url, body, token, Collections.emptyMap());
        System.out.println( options.getResult());
        assertEq(200,options.getResponseCode());
    }


}
