package org.rapla.dagger;

import dagger.Module;
import dagger.Provides;
import org.rapla.server.StartupParams;

@Module
public class DaggerRaplaServerStartupModule
{
    StartupParams params;

    public DaggerRaplaServerStartupModule(StartupParams params)
    {
        this.params = params;
    }

    @Provides
    public StartupParams provide()
    {
        return params;
    }
}
