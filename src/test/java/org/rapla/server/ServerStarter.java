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
    private Map<String, MembersInjector> membersInjector;

    @Inject
    public ServerStarter(Map<String, MembersInjector> membersInjector)
    {
        this.membersInjector = membersInjector;
    }
    
    @Override
    public Map<String, MembersInjector> getMembersInjector()
    {
        return membersInjector;
    }

    public String start()
    {
       return "server";
    }
}
