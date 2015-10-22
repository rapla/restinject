package org.rapla.inject.test;

import java.util.Map;

import javax.inject.Inject;

import org.rapla.inject.extension.ExampleExtensionPoint;

public class Rapla
{
    private Map<String, ExampleExtensionPoint> exampelMap;

    @Inject
    public Rapla(Map<String, ExampleExtensionPoint> exampelMap)
    {
        this.exampelMap = exampelMap;

    }

    public void print()
    {
        System.out.println(exampelMap.toString());
    }
}