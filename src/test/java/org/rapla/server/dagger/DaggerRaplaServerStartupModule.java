package org.rapla.server.dagger;

import dagger.Module;
import dagger.Provides;
import org.rapla.logger.ConsoleLogger;
import org.rapla.logger.Logger;
import org.rapla.server.StartupParams;

@Module
public class DaggerRaplaServerStartupModule
{
    StartupParams params;
    Logger logger;

    public DaggerRaplaServerStartupModule(StartupParams params)
    {
        this.params = params;
        this.logger = new ConsoleLogger();
    }

    @Provides
    public StartupParams provideParams()
    {
        return params;
    }

    @Provides
    public Logger provideLogger() { return logger;}


}
