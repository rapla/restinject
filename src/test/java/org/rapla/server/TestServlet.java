package org.rapla.server;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.rapla.client.standalone.StandaloneConnector;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.Injector;
import org.rapla.inject.raplainject.SimpleRaplaInjector;
import org.rapla.inject.scanning.ScanningClassLoader;
import org.rapla.inject.scanning.ServiceInfLoader;
import org.rapla.logger.ConsoleLogger;
import org.rapla.logger.Logger;
import org.rapla.rest.server.provider.resteasy.ResteasyMembersInjector;
import org.rapla.server.rest.RestTestApplication;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestServlet extends HttpServlet
{

    HttpServletDispatcher dispatcher;
    private Injector membersInjector;
    private final StandaloneConnector standaloneConnector;
    public TestServlet()
    {
        this(null);
    }

    public TestServlet(StandaloneConnector standaloneConnector)
    {
        this.standaloneConnector = standaloneConnector;
    }

    protected String getPrefix()
    {
        return "";
    }
    @Override public void init(ServletConfig config) throws ServletException
    {
        System.out.println("Starting init");

        final ServletContext context = config.getServletContext();
        dispatcher = new HttpServletDispatcher();
        dispatcher.init(new ServletConfig()
        {
            @Override public String getServletName()
            {
                return config.getServletName();
            }

            @Override public ServletContext getServletContext()
            {
                return config.getServletContext();
            }

            @Override public String getInitParameter(String name)
            {
                switch ( name)
                {
                    case "resteasy.servlet.mapping.prefix": return getPrefix() + "/rapla";
                    case "resteasy.use.builtin.providers": return  "true";
                    case "javax.ws.rs.Application": return RestTestApplication.class.getCanonicalName();
                }
                return config.getInitParameter( name);
            }

            @Override public Enumeration<String> getInitParameterNames()
            {
                return config.getInitParameterNames();
            }
        });
        super.init(config);
        System.out.println("Init done ");
        StartupParams params  = new StartupParams();

        // We could either use dagger for depenedency injection
        //membersInjector = new ReflectionMembersInjector( RaplaServerComponent.class, mod);

        Logger logger = new ConsoleLogger();
        // or the simple Rapla injector
        SimpleRaplaInjector container = new SimpleRaplaInjector(logger);
        container.addComponentInstance( StartupParams.class,params);
        try
        {
            // we can use the MetaInfService Loader
            String servicelist = InjectionContext.MODULE_LIST_LOCATION;
            ScanningClassLoader.LoadingFilter filter = null;
            final ScanningClassLoader.LoadingResult loadingResult = new ServiceInfLoader().loadClassesFromServiceInfFile(filter,servicelist);
            container.initFromClasses(InjectionContext.server,loadingResult.getClasses());
            // or the re
//            Set<Class> classes = new HashSet<>();
//            classes.add( RemoteSessionImpl.class);
//            classes.add(TestSingletonImpl.class);
//            container.initFromClasses( InjectionContext.server, classes);
        }
        catch (Exception e)
        {
            throw new ServletException( e);
        }
        membersInjector = container.getMembersInjector( );
    }


    @Override protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            System.out.println("QueryString " + request.getQueryString() + " Test param " + request.getParameter("param"));
            request.setAttribute(ResteasyMembersInjector.INJECTOR_CONTEXT, membersInjector);
            Map<String, String> headers = new LinkedHashMap<>();
            final Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements())
            {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
            System.out.println("service request full " + request.toString() + " uri: " + request.getRequestURI() + " context: " + request.getContextPath() + " \npathInfo "
                    + request.getPathInfo() + " \nservlet path " + request.getServletPath() + " \nHeaders: " + headers.toString());
            //response.addHeader("Access-Control-Allow-Origin","*");
            //        if ( request.getMethod().toLowerCase().equals("options"))
            //        {
            ////            response.addHeader("Access-Control-Allow-Origin","*");
            //            response.setStatus(200);
            //            return;
            //        }

            try
            {
                dispatcher.service(request, response);
            }
            catch (ServletException | IOException ex)
            {
                throw ex;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        finally
        {
            if ( standaloneConnector != null)
            {
                standaloneConnector.requestFinished();
            }
        }

    }


}