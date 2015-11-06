package org.rapla.inject.component.javaclient;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.ComponentStarter;

@RunWith(JUnit4.class)
public class JavaClientComponentTest
{
    
    @Test
    public void startGeneratedServerComponent()
    {
        ComponentStarter starter = org.rapla.client.swing.dagger.DaggerRaplaJavaClientComponent.create().getComponentStarter();
        Assert.assertEquals(JavaClientStarter.class, starter.getClass());
    }

}
