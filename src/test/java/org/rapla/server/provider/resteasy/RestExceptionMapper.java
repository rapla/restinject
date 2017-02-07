package org.rapla.server.provider.resteasy;

import org.jboss.resteasy.spi.ApplicationException;
import org.rapla.logger.RaplaBootstrapLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RestExceptionMapper implements ExceptionMapper<ApplicationException>
{
    HttpServletRequest request;
    public RestExceptionMapper(@Context HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public Response toResponse(ApplicationException container)
    {
        Throwable exception = container.getCause();
        try
        {
            RaplaBootstrapLogger.createRaplaLogger().error(exception.getMessage(), exception);
        }
        catch (Throwable ex)
        {
        }
        final Response.ResponseBuilder entity = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exception);
        final Response build = entity.build();
        return build;
    }
}
