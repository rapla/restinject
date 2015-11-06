package org.rapla.inject.component.server;

import javax.inject.Inject;

import org.rapla.inject.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context=InjectionContext.server, of=ComponentStarter.class)
public class ServerStarter implements ComponentStarter
{
    @Inject
    public ServerStarter()
    {
    }
}
