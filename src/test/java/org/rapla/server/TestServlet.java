package org.rapla.server;

import org.rapla.dagger.DaggerRaplaServerStartupModule;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TestServlet extends HttpServlet
{
    TestServer testServer;
    @Override public void init() throws ServletException
    {
        super.init();
        System.out.println("Init done ");
        StartupParams params  = new StartupParams();
        testServer = org.rapla.server.dagger.DaggerRaplaServerComponent.builder().daggerRaplaServerStartupModule(new DaggerRaplaServerStartupModule(params)).build().getTestServer();
    }

    @Override protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            testServer.service(request, response);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ServletException( e);
        }
    }


}
