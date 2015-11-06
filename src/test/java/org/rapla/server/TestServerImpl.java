package org.rapla.server;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.server.JsonServlet;
import org.rapla.jsonrpc.server.WebserviceCreator;

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
    Map<String,JsonServlet> servletMap = new HashMap<String, JsonServlet>();
    private final StartupParams params;
    @Inject
    public TestServerImpl(StartupParams params){
        this.params = params;
    }

    public void service(HttpServletRequest request, HttpServletResponse response,Map<String,WebserviceCreator> webserviceMap) throws Exception
    {
        System.out.println("Request " + request.toString());
        String serviceAndMethodName = getServiceAndMethodName(request);
        final JsonServlet servlet = getJsonServlet(request, serviceAndMethodName);
        Class<?> role = servlet.getInterfaceClass();
        final WebserviceCreator webserviceCreator = webserviceMap.get(role.getCanonicalName());
        final Object service = webserviceCreator.create(request, response);
        ServletContext servletContext = request.getServletContext();
        servlet.service(request, response, servletContext, service);
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
