package org.rapla.client.standalone;

import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.client.AbstractProxyTest;
import org.rapla.client.swing.MyCustomConnector;
import org.rapla.common.ExampleService;
import org.rapla.common.ExampleService_JavaJsonProxy;
import org.rapla.common.ExampleSimpleService;
import org.rapla.common.ExampleSimpleService_JavaJsonProxy;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.swing.HTTPConnector;
import org.rapla.rest.client.swing.JavaClientServerConnector;
import org.rapla.rest.client.swing.JsonRemoteConnector;
import org.rapla.server.TestServlet;

import java.io.File;

@RunWith(JUnit4.class)
//@Ignore
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
        localConnector = new LocalConnector(server);
        server.addConnector(localConnector);
        String contextPath = "/";
        WebAppContext context = new WebAppContext(server, contextPath, "/");
        context.setResourceBase(webappFolder.getAbsolutePath());
        context.setMaxFormContentSize(64000000);

        final ServletHolder servletHolder = new ServletHolder(TestServlet.class);
        final StandaloneConnector connector = createConnector(localConnector);
        JavaClientServerConnector.setJsonRemoteConnector(connector);
        servletHolder.setServlet(new TestServlet(connector));
        context.addServlet(servletHolder, "/*");
        server.start();
    }

    @After
    public void tearDown() throws Exception
    {
        JavaClientServerConnector.setJsonRemoteConnector(new HTTPConnector());
        server.removeConnector( localConnector );
        server.stop();
        localConnector.destroy();
    }

    StandaloneConnector createConnector(final LocalConnector connector)
    {
        return new StandaloneConnector(connector);
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
