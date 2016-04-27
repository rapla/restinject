package org.rapla.client.gwt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.common.ComponentStarter;

@RunWith(JUnit4.class)
public class ComponentTest
{
    @Test
    public void startGeneratedServerComponent()
    {
        ComponentStarter starter = org.rapla.client.gwt.dagger.DaggerRaplaGwtComponent.create().getComponentStarter();
        Assert.assertEquals("gwt", starter.start());
    }

}
