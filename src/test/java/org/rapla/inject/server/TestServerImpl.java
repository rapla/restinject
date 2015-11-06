package org.rapla.inject.server;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.TestServer;
import org.rapla.inject.extension.ExampleExtensionPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

@DefaultImplementation(of=TestServer.class,context = InjectionContext.server)
@Singleton
public class TestServerImpl implements TestServer
{
    Map<String, ExampleExtensionPoint> exampleMap;
    @Inject
    public TestServerImpl(Map<String, ExampleExtensionPoint> exampleMap, Set<ExampleExtensionPoint> exampleSet)
    {
        this.exampleMap = exampleMap;
    }
    public String getTest()
    {
        return exampleMap.toString();
    }
}
