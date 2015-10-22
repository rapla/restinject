package org.rapla.inject.test;

import org.rapla.dagger.DaggerGwtModule;

import dagger.Component;

@Component(modules = { DaggerGwtModule.class })
public interface GwtInjectionTest
{
    Rapla getRapla();
}