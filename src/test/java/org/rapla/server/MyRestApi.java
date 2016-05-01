package org.rapla.server;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("auth")
public class MyRestApi
{
    @GET
    @Path("{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public String test(@PathParam("username") String username, @QueryParam("password") String password)
    {
        if (password != null && password.equals("secret"))
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
    public String test_(@PathParam("username") String username, @QueryParam("password") String password) throws IOException
    {
        if (password != null && password.equals("secret"))
        {
            return "Hello " + username;
        }
        else
        {
            throw new IOException("login failed");
        }
    }

}
