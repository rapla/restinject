package org.rapla.client.gwt;

import javax.inject.Singleton;

import org.junit.Assert;
import org.rapla.client.dagger.DaggerRaplaClientModule;
import org.rapla.client.gwt.dagger.DaggerRaplaGwtModule;

import dagger.Component;
import junit.framework.TestCase;
import org.rapla.common.DefaultImplFor2Interfaces;
import org.rapla.common.ImplInterfaceUser;
import org.rapla.common.OtherInterfaceUser;
import org.rapla.common.dagger.DaggerRaplaCommonModule;

public class InjectionTest extends TestCase
{
    @Component(modules={ DaggerRaplaCommonModule.class,DaggerRaplaClientModule.class,DaggerRaplaGwtModule.class})
    @Singleton
    public interface GwtInterface{
        ImplInterfaceUser getImplInterfaceUser();
        OtherInterfaceUser getOtherInterfaceUser();
    }
    
    public void testServerInjection()
    {
        GwtInterface si = DaggerInjectionTest_GwtInterface.create();
        Assert.assertTrue(si.getImplInterfaceUser().isImplInterfaceClass(DefaultImplFor2Interfaces.class));
        Assert.assertTrue(si.getOtherInterfaceUser().isOtherInterfaceClass(OtherGwtInterfaceImpl.class));
    }

}
