package org.rapla.common.extension;

import dagger.Component;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

@RunWith(JUnit4.class)
public class TestExtensionPointInjection
{

    @Component(modules={org.rapla.server.dagger.DaggerRaplaServerModule.class, org.rapla.common.dagger.DaggerRaplaCommonModule.class})
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
