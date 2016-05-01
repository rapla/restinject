package org.rapla.common;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("ExampleSimpleService")
public interface ExampleSimpleService
{
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("sayHello")
    String sayHello(@QueryParam("param") String param);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("translation")
    List<String> translations(@HeaderParam("id") String id);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("exception")
    List<String> exception();

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendString")
    String sendString(@QueryParam("str")String param);

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendInt")
    Integer sendInt(@QueryParam("Int")Integer param);

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendPrimInt")
    int sendPrimInt(@QueryParam("int")int param);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendBool")
    Boolean sendBool(@QueryParam("Bool")Boolean param);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendPrimBool")
    boolean sendPrimBool(@QueryParam("bool")boolean param);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendPrimDouble")
    double sendPrimDouble(@QueryParam("double")double param);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendDouble")
    Double sendDouble(@QueryParam("double")Double param);
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("sendChar")
    Character sendChar(@QueryParam("char")Character param);
}