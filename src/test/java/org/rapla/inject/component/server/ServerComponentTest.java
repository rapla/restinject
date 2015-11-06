package org.rapla.inject.component.server;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.inject.ComponentStarter;

@RunWith(JUnit4.class)
public class ServerComponentTest
{
    
    @Test
    public void startGeneratedServerComponent()
    {
        ComponentStarter starter = org.rapla.server.dagger.DaggerServerComponent.create().getStarter();
        Assert.assertEquals(ServerStarter.class, starter.getClass());
    }

}
