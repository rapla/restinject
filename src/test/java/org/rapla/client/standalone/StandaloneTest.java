package org.rapla.client.standalone;

import java.io.File;

import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.client.AbstractProxyTest;
import org.rapla.client.swing.MyCustomConnector;
import org.rapla.common.ExampleService;
import org.rapla.common.ExampleService_JavaJsonProxy;
import org.rapla.common.ExampleSimpleService;
import org.rapla.common.ExampleSimpleService_JavaJsonProxy;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.swing.AbstractLocalJsonConnector;
import org.rapla.rest.client.swing.HTTPConnector;
import org.rapla.rest.client.swing.JavaClientServerConnector;
import org.rapla.rest.client.swing.JsonRemoteConnector;
import org.rapla.rest.server.ServiceInfLoader;
import org.rapla.server.TestServlet;

@RunWith(JUnit4.class)
public class StandaloneTest extends AbstractProxyTest
{

    private Server server;
    private LocalConnector localConnector;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        File webappFolder = new File("test");
        server = new Server();
        localConnector = new LocalConnector();
        server.addConnector(localConnector);
        String contextPath = "/";
        WebAppContext context = new WebAppContext(server, contextPath, "/");
        context.setInitParameter("resteasy.servlet.mapping.prefix", "/rapla");
        context.setInitParameter("resteasy.use.builtin.providers", "false");
        context.setInitParameter("javax.ws.rs.Application", ServiceInfLoader.class.getCanonicalName());
        context.setResourceBase(webappFolder.getAbsolutePath());
        context.setMaxFormContentSize(64000000);

        final ServletHolder servletHolder = new ServletHolder(TestServlet.class);
        servletHolder.setServlet(new TestServlet());
        context.addServlet(servletHolder, "/*");
        JavaClientServerConnector.setJsonRemoteConnector(createConnector(localConnector));
        server.start();
    }

    @After
    public void tearDown() throws Exception
    {
        server.stop();
        JavaClientServerConnector.setJsonRemoteConnector(new HTTPConnector());
        localConnector.close();
    }

    JsonRemoteConnector createConnector(final LocalConnector connector)
    {
        return new AbstractLocalJsonConnector()
        {
            @Override
            protected String doSend(String rawHttpRequest)
            {
                try
                {
                    final ByteArrayBuffer requestsBuffer;
                    requestsBuffer = new ByteArrayBuffer(rawHttpRequest.getBytes("UTF-8"));
                    connector.setMaxIdleTime(30000);
                    final String responses = connector.getResponses(requestsBuffer, false).toString("UTF-8");
                    return responses;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    protected CustomConnector createConnector()
    {
        return new MyCustomConnector();
    }

    @Override
    protected ExampleService createExampleServiceProxy()
    {
        return new ExampleService_JavaJsonProxy(connector);
    }

    @Override
    protected ExampleSimpleService createExampleSimpleServiceProxy()
    {
        return new ExampleSimpleService_JavaJsonProxy(connector);
    }

    @Override
    public void assertEq(Object o1, Object o2)
    {
        Assert.assertEquals(o1, o2);
    }

    @Override
    public void fail_(String message)
    {
        Assert.fail(message);
    }

}
