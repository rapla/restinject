package org.rapla.server;

import javax.inject.Singleton;

import org.junit.Assert;
import org.rapla.server.dagger.DaggerRaplaServerModule;

import dagger.Component;
import junit.framework.TestCase;
import org.rapla.common.DefaultImplFor2Interfaces;
import org.rapla.common.ImplInterfaceUser;
import org.rapla.common.OtherInterfaceUser;

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
        Assert.assertTrue(si.getImplInterfaceUser().isImplInterfaceClass(DefaultImplFor2Interfaces.class));
        Assert.assertTrue(si.getOtherInterfaceUser().isOtherInterfaceClass(DefaultImplFor2Interfaces.class));
    }
}
