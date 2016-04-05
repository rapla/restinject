package org.rapla.server;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.rapla.common.dagger.CycleTest;
import org.rapla.server.rest.RestTestApplication;
import org.rapla.server.rest.filter.InjectionFilter;

/**
 * Created by Christopher on 21.10.2015.
 */
public class ServletTestContainer
{

    static public Server createServer() throws Exception
    {

        int port = 8052;
        File webappFolder = new File("test");
        Server jettyServer = new Server(port);
        WebAppContext context = new WebAppContext(jettyServer, "rapla", "/");
        context.setResourceBase(webappFolder.getAbsolutePath());
        context.setMaxFormContentSize(64000000);
//        context.setInitParameter("resteasy.servlet.mapping.prefix", "/rest");
//        context.setInitParameter("resteasy.use.builtin.providers", "false");
//        context.setInitParameter("javax.ws.rs.Application", RestTestApplication.class.getCanonicalName());
        context.addServlet(new ServletHolder(TestServlet.class), "/rest/*");
        //context.addServlet(new ServletHolder(org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher.class), "/rest/*");
//        context.addServlet(new ServletHolder(mainServlet), "/*");
        //context.addFilter(InjectionFilter.class, "/rest/*", EnumSet.copyOf(Arrays.asList(DispatcherType.values())));
        jettyServer.start();
        Handler[] childHandlers = context.getChildHandlersByClass(ServletHandler.class);
        final ServletHandler childHandler = (ServletHandler) childHandlers[0];
        final ServletHolder[] servlets = childHandler.getServlets();
        ServletHolder servlet = servlets[0];

        return jettyServer;
    }
}
