package org.rapla.rest.server.provider.exception;

import org.rapla.logger.RaplaBootstrapLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RestExceptionMapper implements ExceptionMapper<Throwable>
{

    public RestExceptionMapper(@Context HttpServletRequest request)
    {
    }

    @Override
    public Response toResponse(Throwable exception)
    {
        try
        {
            RaplaBootstrapLogger.createRaplaLogger().error(exception.getMessage(), exception);
        }
        catch (Throwable ex)
        {
        }
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception).build();
    }
}
