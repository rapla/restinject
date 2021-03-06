package org.rapla.client.gwt;

import dagger.Component;
import junit.framework.TestCase;
import org.rapla.common.extension.ExampleExtensionPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

public class MyAppTest extends TestCase
{
    @Component(modules = {org.rapla.client.gwt.dagger.DaggerRaplaGwtModule.class })
    @Singleton
    public interface GwtInjectionInterface
    {
        Rapla getRapla();
    }

    public void testApplication() throws Exception
    {
        DaggerMyAppTest_GwtInjectionInterface.create().getRapla().print();
    }

    static public class Rapla
    {
        private Set< ExampleExtensionPoint> exampelMap;
        //private Set<ExampleExtensionPoint> exampleSet;

        @Inject
        public Rapla(Set< ExampleExtensionPoint> exampleMap)
        {
            this.exampelMap = exampleMap;
            //this.exampleSet = exampleSet;

        }

        public void print()
        {
            System.out.println("Map: " + exampelMap.toString());
            //System.out.println("Set: "+exampleSet.toString());
        }
    }
}
