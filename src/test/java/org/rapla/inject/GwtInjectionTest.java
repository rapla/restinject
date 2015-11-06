package org.rapla.inject;

import javax.inject.Singleton;

import org.junit.Assert;
import org.rapla.dagger.DaggerRaplaGwtModule;

import dagger.Component;
import junit.framework.TestCase;

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
        Assert.assertTrue(si.getImplInterfaceUser().isImplInterfaceClass(DefaultImplTest.class));
        Assert.assertTrue(si.getOtherInterfaceUser().isOtherInterfaceClass(OtherGwtInterfaceImpl.class));
    }

}
