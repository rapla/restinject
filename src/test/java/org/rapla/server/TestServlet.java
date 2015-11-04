package org.rapla.server;

import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.server.JsonServlet;

import javax.inject.Provider;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Christopher on 15.10.2015.
 */
public class TestServlet extends HttpServlet
{
    Map<String,JsonServlet> servletMap = new HashMap<String, JsonServlet>();

    ServerComponent serverComponent;
    @Override public void init() throws ServletException
    {
        super.init();
        System.out.println("Init done ");
        serverComponent = org.rapla.server.DaggerServerComponent.create();
    }

    @Override protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        System.out.println("Request " + request.toString());
        String serviceAndMethodName = getServiceAndMethodName(request);
        try
        {
            final JsonServlet servlet = getJsonServlet(request, serviceAndMethodName);
            Class<?> role = servlet.getInterfaceClass();
            Provider<Object> requestComponent = serverComponent.getWebservice().getList().find(request, response,role.getCanonicalName());
            ServletContext servletContext = getServletContext();
            final Object service = requestComponent.get();//get().getObject(role.getCanonicalName());
            servlet.service(request, response, servletContext, service);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException( e);
        }
    }

    protected String getServiceAndMethodName(HttpServletRequest request) {
        String requestURI =request.getPathInfo();
        String path = "/json/";
        if ( requestURI != null)
        {
            int rpcIndex = requestURI.indexOf(path);
            String serviceAndMethodName = requestURI.substring(rpcIndex + path.length()).trim();
            return serviceAndMethodName;
        }
        return request.getRequestURI();
    }

    private JsonServlet getJsonServlet(HttpServletRequest request,String serviceAndMethodName) throws Exception {
        if  ( serviceAndMethodName == null || serviceAndMethodName.length() == 0) {
            throw new Exception("Servicename missing in url");
        }
        int indexRole = serviceAndMethodName.indexOf( "/" );
        String interfaceName;
        if ( indexRole > 0 )
        {
            interfaceName= serviceAndMethodName.substring( 0, indexRole );
            if ( serviceAndMethodName.length() >= interfaceName.length())
            {
                String methodName = serviceAndMethodName.substring( indexRole + 1 );
                request.setAttribute(JsonServlet.JSON_METHOD, methodName);
            }
        }
        else
        {
            interfaceName = serviceAndMethodName;
        }
        JsonServlet servlet = servletMap.get( interfaceName);
        if ( servlet == null)
        {
            // security check, we need to be sure a webservice with the name is provide before we load the class
            Class<?> interfaceClass =  Class.forName(interfaceName, true, getClass().getClassLoader());
            final Class webserviceAnnotation = RemoteJsonMethod.class;
            if (interfaceClass.getAnnotation(webserviceAnnotation) == null)
            {
                throw new Exception(interfaceName + " is not a webservice. Did you forget the proxy " + webserviceAnnotation.getName() + "?");
            }
            // Test if service is found
            servlet = new JsonServlet( interfaceClass);
            servletMap.put( interfaceName, servlet);
        }
        return servlet;
    }

}
