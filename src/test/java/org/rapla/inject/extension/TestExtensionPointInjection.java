package org.rapla.inject.extension;

import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.dagger.DaggerRaplaServerModule;

import dagger.Component;

@RunWith(JUnit4.class)
public class TestExtensionPointInjection 
{

    @Component(modules=DaggerRaplaServerModule.class)
    @Singleton
    public interface ExtensionContext{
        Set<ExampleExtensionPoint> getExtensionPointImpls(); 
        Map<String, ExampleExtensionPoint> getExtensionPointImplsAsMap(); 
    }
    
    @Test
    public void injection()
    {
        ExtensionContext ec= DaggerTestExtensionPointInjection_ExtensionContext.create();
        Assert.assertEquals(2, ec.getExtensionPointImpls().size());
        Assert.assertEquals(2, ec.getExtensionPointImplsAsMap().size());
    }
}
