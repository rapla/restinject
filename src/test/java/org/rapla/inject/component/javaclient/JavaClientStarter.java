package org.rapla.inject.component.javaclient;

import javax.inject.Inject;

import org.rapla.inject.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context=InjectionContext.swing, of=ComponentStarter.class)
public class JavaClientStarter implements ComponentStarter
{

    @Inject
    public JavaClientStarter()
    {
    }
}
