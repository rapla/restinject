package org.rapla.server;

import org.rapla.jsonrpc.common.RemoteJsonMethod;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.PageAttributes;
import java.io.IOException;
import java.io.PrintWriter;

@Path("auth")
@RemoteJsonMethod
public class MyRestPage
{
    @Inject
    public MyRestPage()
    {
    }

    @GET
    @Path("{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public String test(@PathParam("username") String username, @QueryParam("password") String password)
    {
        if ( password != null && password.equals("secret"))
        {
            return "Hello " + username;
        }
        else
        {
            return "login failed";
        }
    }

    @GET
    @Path("{username}")
    @Produces(MediaType.TEXT_HTML)
    public void test_(@PathParam("username") String username, @QueryParam("password") String password,@Context HttpServletResponse response) throws IOException
    {
        try(PrintWriter out = response.getWriter())
        {
            if (password != null && password.equals("secret"))
            {
                out.print("Hello " + username);
            }
            else
            {
                response.sendError(Response.Status.FORBIDDEN.getStatusCode(),"login failed");
            }
        }
    }
}
