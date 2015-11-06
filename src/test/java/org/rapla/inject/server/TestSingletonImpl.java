package org.rapla.inject.server;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.TestSingleton;
import org.rapla.inject.extension.ExampleExtensionPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

@DefaultImplementation(of=TestSingleton.class,context = InjectionContext.server)
@Singleton
public class TestSingletonImpl implements TestSingleton
{
    Map<String, ExampleExtensionPoint> exampleMap;
    @Inject
    public TestSingletonImpl(Map<String, ExampleExtensionPoint> exampleMap, Set<ExampleExtensionPoint> exampleSet)
    {
        this.exampleMap = exampleMap;
    }
    public String getTest()
    {
        return exampleMap.toString();
    }
}
