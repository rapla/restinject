package org.rapla.inject.test;

import dagger.Component;
import junit.framework.TestCase;
import org.rapla.dagger.DaggerGwtModule;
import org.rapla.inject.extension.ExampleExtensionPoint;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class MyAppTest extends TestCase
{
    @Component(modules = { DaggerGwtModule.class })
    public interface GwtInjectionTest
    {
        Rapla getRapla();
    }

    public void testApplication() throws Exception
    {
        DaggerMyAppTest_GwtInjectionTest.create().getRapla().print();
    }

    static public class Rapla
    {
        private Map<String, ExampleExtensionPoint> exampelMap;
        private Set<ExampleExtensionPoint> exampleSet;

        @Inject
        public Rapla(Map<String, ExampleExtensionPoint> exampleMap, Set<ExampleExtensionPoint> exampleSet)
        {
            this.exampelMap = exampleMap;
            this.exampleSet = exampleSet;

        }

        public void print()
        {
            System.out.println("Map: "+exampelMap.toString());
            System.out.println("Set: "+exampleSet.toString());
        }
    }
}
