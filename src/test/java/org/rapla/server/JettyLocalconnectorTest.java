package org.rapla.server;

import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.client.swing.MyCustomConnector;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest_JavaJsonProxy;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.swing.AbstractLocalJsonConnector;
import org.rapla.rest.client.swing.JavaClientServerConnector;
import org.rapla.rest.client.swing.JsonRemoteConnector;
import org.rapla.rest.server.RestApplication;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class) public class JettyLocalconnectorTest
{
    Server server;

    @Before public void setUp() throws Exception
    {
        File webappFolder = new File("test");
        server = new Server();
        LocalConnector connector = new LocalConnector();
        server.addConnector(connector);

        String contextPath = "/";
        WebAppContext context = new WebAppContext(server, contextPath, "/");
        //        context.addFilter(org.rapla.server.HTTPMethodOverrideFilter.class, "/rapla/*", null);
        context.addEventListener(new ResteasyBootstrap());
        //        final Filter filter = new Filter()
        //        {
        //            @Override
        //            public void init(FilterConfig filterConfig) throws ServletException
        //            {
        //                // do not init context as given from outside
        //            }
        //
        //            @Override
        //            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
        //            {
        //                request.setAttribute(RaplaRestDaggerContextProvider.RAPLA_CONTEXT, serverServiceImpl.getMembersInjector());
        //                chain.doFilter(request, response);
        //            }
        //
        //            @Override
        //            public void destroy()
        //            {
        //
        //            }
        //        };
        //        final FilterHolder holder = new FilterHolder(filter);
        //        context.addFilter(holder, "/*", EnumSet.allOf(DispatcherType.class));
        context.setInitParameter("resteasy.servlet.mapping.prefix", "/rapla");
        context.setInitParameter("resteasy.use.builtin.providers", "false");
        context.setInitParameter("javax.ws.rs.Application", RestApplication.class.getCanonicalName());
        context.setResourceBase(webappFolder.getAbsolutePath());
        context.setMaxFormContentSize(64000000);

        final ServletHolder servletHolder = new ServletHolder(TestServlet.class);
        servletHolder.setServlet(new TestServlet());
        context.addServlet(servletHolder, "/*");
        server.start();
        Handler[] childHandlers = context.getChildHandlersByClass(ServletHandler.class);
        final ServletHandler childHandler = (ServletHandler) childHandlers[0];
        final ServletHolder[] servlets = childHandler.getServlets();
        ServletHolder servlet = servlets[0];
        JavaClientServerConnector.setJsonRemoteConnector(createConnector(connector));

        /*
        URL server = new URL("http://127.0.0.1:"+port+"/rapla/auth");
        HttpURLConnection connection = (HttpURLConnection)server.openConnection();
        int timeout = 10000;
        int interval = 200;
        for ( int i=0;i<timeout / interval;i++)
        {
            try
            {
                connection.connect();
            }
            catch (ConnectException ex) {
                Thread.sleep(interval);
            }
        }

        return jettyServer;
        */
    }

    @After public void tearDown() throws Exception
    {
        server.stop();
    }

    JsonRemoteConnector createConnector(LocalConnector connector)
    {
        return new AbstractLocalJsonConnector()
        {
            @Override protected String doSend(String rawHttpRequest)
            {
                try
                {
                    final long start = System.currentTimeMillis();
                    final ByteArrayBuffer requestsBuffer;
                    requestsBuffer = new ByteArrayBuffer(rawHttpRequest.getBytes("UTF-8"));
                    final String responses = connector.getResponses(requestsBuffer, false).toString("UTF-8");
                    final long end = System.currentTimeMillis();
                    System.out.println(responses);
                    System.out.println("Took" + (end - start) + "ms");
                    return responses;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test public void testHttp() throws Exception
    {
        CustomConnector customConnector = new MyCustomConnector();
        AnnotationProcessingTest test = new AnnotationProcessingTest_JavaJsonProxy(customConnector);
        AnnotationProcessingTest.Parameter param = new AnnotationProcessingTest.Parameter();
        param.setActionIds(Arrays.asList(new Integer[] {1,2}));
        final List<AnnotationProcessingTest.Result> results = test.sayHello(param);
        System.out.println( results);
    }

    /*
    static public class MyTestServlet extends HttpServlet
    {
        @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            final ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.println("Hello World 2");
            outputStream.close();
        }
    }
    */
}

