package org.rapla.server;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.common.ComponentStarter;
import org.rapla.dagger.DaggerRaplaServerStartupModule;
import org.rapla.server.dagger.RaplaServerComponent;

@RunWith(JUnit4.class)
public class ServerComponentTest
{
    @Test
    public void startGeneratedServerComponent()
    {
        StartupParams params = new StartupParams();
        RaplaServerComponent serverComponent = org.rapla.server.dagger.DaggerRaplaServerComponent.builder().daggerRaplaServerStartupModule(new DaggerRaplaServerStartupModule(params)).build();
        ComponentStarter starter = serverComponent.getComponentStarter();
        Assert.assertEquals("server",starter.start());
    }

}
