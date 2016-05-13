package org.rapla.server;

import org.rapla.common.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import dagger.MembersInjector;

import java.util.Map;

import javax.inject.Inject;

@DefaultImplementation(context=InjectionContext.server, of=ComponentStarter.class,export = true)
public class ServerStarter implements ComponentStarter
{

    @Inject
    public ServerStarter()
    {
    }
    

    public String start()
    {
       return "server";
    }
}
