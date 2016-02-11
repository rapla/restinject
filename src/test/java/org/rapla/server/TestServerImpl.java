package org.rapla.server;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.server.JsonServlet;
import org.rapla.jsonrpc.server.WebserviceCreator;
import org.rapla.jsonrpc.server.WebserviceCreatorMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


@DefaultImplementation(of=TestServer.class,context = InjectionContext.server,export = true)
@Singleton
public class TestServerImpl implements  TestServer
{
    Map<Class,JsonServlet> servletMap = new HashMap<Class, JsonServlet>();
    private final StartupParams params;
    Map<String,WebserviceCreator> webserviceMap;
    @Inject
    public TestServerImpl(StartupParams params,WebserviceCreatorMap webserviceMap){
        this.params = params;
        this.webserviceMap = webserviceMap.asMap();
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        System.out.println("Request " + request.toString());
        String path = null;
        String appendix = null;
        String requestURI =request.getPathInfo();
        String subPath;
        if ( !requestURI.startsWith("/rapla/"))
        {
            if ( requestURI.startsWith("/"))
            {
                subPath = requestURI.substring(1);
            }
            else
            {
                subPath = requestURI;
            }
        }
        else
        {
            subPath= requestURI.substring("/rapla/".length());
        }
        for (String key:webserviceMap.keySet())
        {
            if (subPath.startsWith(key))
            {
                path = key;
                if (subPath.length() > key.length())
                {
                    appendix = subPath.substring(key.length() + 1);
                }
            }
        }
        if ( path == null)
        {
            throw new IllegalArgumentException("No webservice found for " + path + " full request uri " + requestURI + " subpath " + subPath);
        }
        final WebserviceCreator webserviceCreator = webserviceMap.get(path);
        Class serviceClass = webserviceCreator.getServiceClass();
        final JsonServlet servlet = getJsonServlet(request, serviceClass);
        final Object service = webserviceCreator.create(request, response);
        ServletContext servletContext = request.getServletContext();
        servlet.service(request, response, servletContext, service, appendix);
    }

    /*
    protected String getServiceAndMethodName(HttpServletRequest request) {
        String requestURI =request.getPathInfo();
        //String path = "/json/";
        String path = "/rapla/";
        if ( requestURI != null)
        {
            int rpcIndex = requestURI.indexOf(path);
            String serviceAndMethodName = requestURI.substring(rpcIndex + path.length()).trim();
            return serviceAndMethodName;
        }
        return request.getRequestURI();
    }
    */

    private JsonServlet getJsonServlet(HttpServletRequest request,Class interfaceClass) throws Exception {

        JsonServlet servlet = servletMap.get( interfaceClass);
        if ( servlet == null)
        {
            // security check, we need to be sure a webservice with the name is provide before we load the class
            final Class webserviceAnnotation = RemoteJsonMethod.class;
            if (interfaceClass.getAnnotation(webserviceAnnotation) == null)
            {
                throw new Exception(interfaceClass + " is not a webservice. Did you forget the proxy " + webserviceAnnotation.getName() + "?");
            }
            // Test if service is found
            servlet = new JsonServlet( interfaceClass);
            servletMap.put( interfaceClass, servlet);
        }
        return servlet;
    }

}
