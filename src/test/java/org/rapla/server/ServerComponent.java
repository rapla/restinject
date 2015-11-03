package org.rapla.server;

import dagger.Component;
import org.rapla.dagger.DaggerServerModule;
import org.rapla.dagger.DaggerWebserviceComponent;

import javax.inject.Singleton;

@Component(modules ={DaggerServerModule.class})
@Singleton
public interface ServerComponent
{
    DaggerWebserviceComponent getWebservice();
}
