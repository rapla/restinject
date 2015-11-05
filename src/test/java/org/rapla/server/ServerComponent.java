package org.rapla.server;

import dagger.Component;

import javax.inject.Singleton;

@Component(modules ={org.rapla.dagger.DaggerRaplaServerModule.class})
@Singleton
public interface ServerComponent
{
    org.rapla.dagger.DaggerRaplaWebserviceComponent getWebservice();
}
