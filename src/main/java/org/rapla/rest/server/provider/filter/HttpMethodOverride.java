package org.rapla.rest.server.provider.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider @PreMatching public class HttpMethodOverride implements ContainerRequestFilter
{
    @Override public void filter(ContainerRequestContext requestContext) throws IOException
    {
        String receivedMethod = requestContext.getMethod();
        String methodFromHeader = requestContext.getHeaderString("X-HTTP-Method-Override");
        if (methodFromHeader != null && !receivedMethod.equals(methodFromHeader))
        {
            requestContext.setMethod(methodFromHeader);
        }
    }
}
