package org.rapla.client.gwt;

import javax.inject.Singleton;

import org.junit.Assert;
import org.rapla.client.gwt.dagger.DaggerRaplaGwtModule;

import dagger.Component;
import junit.framework.TestCase;
import org.rapla.common.DefaultImplFor2Interfaces;
import org.rapla.common.ImplInterfaceUser;
import org.rapla.common.OtherInterfaceUser;

public class GwtInjectionTest extends TestCase
{
    @Component(modules=DaggerRaplaGwtModule.class)
    @Singleton
    public interface GwtInterface{
        ImplInterfaceUser getImplInterfaceUser();
        OtherInterfaceUser getOtherInterfaceUser();
    }
    
    public void testServerInjection()
    {
        GwtInterface si = DaggerGwtInjectionTest_GwtInterface.create();
        Assert.assertTrue(si.getImplInterfaceUser().isImplInterfaceClass(DefaultImplFor2Interfaces.class));
        Assert.assertTrue(si.getOtherInterfaceUser().isOtherInterfaceClass(OtherGwtInterfaceImpl.class));
    }

}
