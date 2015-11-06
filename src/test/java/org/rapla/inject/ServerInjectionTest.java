package org.rapla.inject;

import javax.inject.Singleton;

import org.junit.Assert;
import org.rapla.dagger.DaggerRaplaServerModule;

import dagger.Component;
import junit.framework.TestCase;

public class ServerInjectionTest extends TestCase
{
    
    @Component(modules=DaggerRaplaServerModule.class)
    @Singleton
    public interface ServerInterface{
        ImplInterfaceUser getImplInterfaceUser();
        OtherInterfaceUser getOtherInterfaceUser();
    }
    
    public void testServerInjection()
    {
        ServerInterface si = DaggerServerInjectionTest_ServerInterface.create();
        Assert.assertTrue(si.getImplInterfaceUser().isImplInterfaceClass(DefaultImplTest.class));
        Assert.assertTrue(si.getOtherInterfaceUser().isOtherInterfaceClass(DefaultImplTest.class));
    }
}
