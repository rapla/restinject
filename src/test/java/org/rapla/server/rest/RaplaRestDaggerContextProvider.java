package org.rapla.server.rest;

import org.jboss.resteasy.spi.validation.GeneralValidator;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider public class RaplaRestDaggerContextProvider implements ContextResolver<GeneralValidator>
{
    final ResteasyMembersInjector raplaRestDaggerListener = new ResteasyMembersInjector();
    @Override public GeneralValidator getContext(Class<?> type)
    {
        return raplaRestDaggerListener;
    }
}
