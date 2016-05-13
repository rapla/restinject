package org.rapla.server;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.common.ComponentStarter;

@RunWith(JUnit4.class)
public class ServerComponentTest
{
    @Test
    public void startGeneratedServerComponent()
    {
        StartupParams params = new StartupParams();
        org.rapla.server.dagger.RaplaServerComponent serverComponent = null;//org.rapla.server.dagger.DaggerRaplaServerComponent.builder().daggerRaplaServerStartupModule(new org.rapla.server.dagger.DaggerRaplaServerStartupModule(params)).build();
        ComponentStarter starter = serverComponent.getComponentStarter();
        Assert.assertEquals("server",starter.start());
    }

}
