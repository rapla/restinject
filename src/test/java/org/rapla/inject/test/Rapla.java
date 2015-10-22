package org.rapla.inject.test;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.rapla.inject.extension.ExampleExtensionPoint;

public class Rapla
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