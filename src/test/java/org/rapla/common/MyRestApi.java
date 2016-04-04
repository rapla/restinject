package org.rapla.common;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("auth")
public interface MyRestApi
{

    @GET
    @Path("{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    String test(@PathParam("username") String username, @QueryParam("password") String password);

    @GET
    @Path("{username}")
    @Produces(MediaType.TEXT_HTML)
    String test_(@PathParam("username") String username, @QueryParam("password") String password) throws IOException;
}
