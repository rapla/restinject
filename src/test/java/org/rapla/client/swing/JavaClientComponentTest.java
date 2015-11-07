package org.rapla.client.swing;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.common.ComponentStarter;
import org.rapla.inject.dagger.DaggerReflectionStarter;

@RunWith(JUnit4.class)
public class JavaClientComponentTest
{
    
    @Test
    public void startGeneratedServerComponent()
    {
        ComponentStarter starter = org.rapla.client.swing.dagger.DaggerRaplaJavaClientComponent.create().getComponentStarter();
        Assert.assertEquals("swing", starter.start());
    }

    @Test
    public void startGeneratedServerComponentReflection() throws Exception
    {
        ComponentStarter starter = DaggerReflectionStarter.startWithReflection(ComponentStarter.class, DaggerReflectionStarter.Scope.JavaClient);
        Assert.assertEquals("swing", starter.start());
    }

}
