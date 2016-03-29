package org.rapla.common;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("SimpleService")
public interface SimpleService
{
    @DELETE
    void sayHello();

    @POST
    boolean isDone();

    @PUT
    int getStepCount();

    @GET
    double getPercentage();
}
