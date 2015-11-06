package org.rapla.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.Servlet;
import java.io.File;

/**
 * Created by Christopher on 21.10.2015.
 */
public class ServletTestContainer
{

    static public Server createServer(Class<? extends Servlet> mainServlet) throws Exception
    {

        int port = 8052;
        File webappFolder = new File("test");
        Server jettyServer = new Server(port);
        WebAppContext context = new WebAppContext(jettyServer, "rapla", "/");
        context.setResourceBase(webappFolder.getAbsolutePath());
        context.setMaxFormContentSize(64000000);

        context.addServlet(new ServletHolder(mainServlet), "/*");
        jettyServer.start();
        Handler[] childHandlers = context.getChildHandlersByClass(ServletHandler.class);
        final ServletHandler childHandler = (ServletHandler) childHandlers[0];
        final ServletHolder[] servlets = childHandler.getServlets();
        ServletHolder servlet = servlets[0];

        return jettyServer;
    }
}
