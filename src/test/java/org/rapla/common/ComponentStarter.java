package org.rapla.common;

import java.util.Map;

import dagger.MembersInjector;
import org.rapla.inject.client.gwt.GwtComponentMarker;

public interface ComponentStarter extends GwtComponentMarker
{
    public String start();
    
    //Map<String, MembersInjector> getMembersInjector();
}
