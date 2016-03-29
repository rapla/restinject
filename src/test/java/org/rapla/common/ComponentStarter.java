package org.rapla.common;

import java.util.Map;

import dagger.MembersInjector;

public interface ComponentStarter
{
    public String start();
    
    Map<String, MembersInjector> getMembersInjector();
}
