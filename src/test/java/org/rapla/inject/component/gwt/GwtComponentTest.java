package org.rapla.inject.component.gwt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.inject.ComponentStarter;

@RunWith(JUnit4.class)
public class GwtComponentTest
{
    
    @Test
    public void startGeneratedServerComponent()
    {
        ComponentStarter starter = org.rapla.client.gwt.dagger.DaggerGwtComponent.create().getStarter();
        Assert.assertEquals(GwtStarter.class, starter.getClass());
    }

}
