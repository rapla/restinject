package org.rapla.common;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("ExampleWithParameterArray")
public interface ExampleWithParameterArray
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("arrayTest")
    Integer[] arrayTest(@HeaderParam("ids") String[] ids);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("listTest")
    List<Integer> arrayTest(@QueryParam("ids") List<String> ids);
}