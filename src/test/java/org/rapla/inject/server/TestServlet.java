package org.rapla.inject.server;

import org.rapla.server.dagger.RaplaServerComponent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Christopher on 15.10.2015.
 */
public class TestServlet extends HttpServlet
{
    org.rapla.server.dagger.RaplaServerComponent serverComponent;
    @Override public void init() throws ServletException
    {
        super.init();
        System.out.println("Init done ");
        serverComponent = org.rapla.server.dagger.DaggerRaplaServerComponent.create();
    }

    @Override protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            serverComponent.getTestServer().service(request, response, serverComponent.getServiceMap());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException( e);
        }
    }


}
