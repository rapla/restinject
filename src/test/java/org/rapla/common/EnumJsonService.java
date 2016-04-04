package org.rapla.common;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("EnumJsonService")
public interface EnumJsonService
{

    public enum TrueFalse
    {
        TRUE, FALSE
    }

    public class Parameter
    {
        private TrueFalse selection;
        private String reason;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{comment}")
    TrueFalse insert(@PathParam("comment")String comment);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    TrueFalse get(Parameter param);

}
