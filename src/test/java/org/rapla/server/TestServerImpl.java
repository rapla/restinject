package org.rapla.server;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@DefaultImplementation(of=TestServer.class,context = InjectionContext.server,export = true)
@Singleton
public class TestServerImpl implements  TestServer
{
    private final StartupParams params;
    @Inject
    public TestServerImpl(StartupParams params){
        this.params = params;
        //this.webserviceMap = webserviceMap.asMap();
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
//        for (String key:webserviceMap.keySet())
//        {
//            if (subPath.startsWith(key))
//            {
//                path = key;
//                if (subPath.length() > key.length())
//                {
//                    appendix = subPath.substring(key.length() + 1);
//                }
//            }
//        }
//        if ( path == null)
//        {
//            throw new IllegalArgumentException("No webservice found for " + path + " full request uri " + requestURI + " subpath " + subPath);
//        }
//        final WebserviceCreator webserviceCreator = webserviceMap.get(path);
//        Class serviceClass = webserviceCreator.getServiceClass();
//        final JsonServlet servlet = getJsonServlet(request, serviceClass);
//        final Object service = webserviceCreator.create(request, response);
//        ServletContext servletContext = request.getServletContext();
//        servlet.service(request, response, servletContext, service, appendix);
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


}