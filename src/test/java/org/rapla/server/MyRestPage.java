package org.rapla.server;

import org.rapla.jsonrpc.common.RemoteJsonMethod;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

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
}
