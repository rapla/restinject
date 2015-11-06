package org.rapla.inject.component.javaclient;

import org.rapla.common.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import javax.inject.Inject;

@DefaultImplementation(context=InjectionContext.swing, of=ComponentStarter.class,export = true)
public class JavaClientStarter implements ComponentStarter
{
    @Inject
    public JavaClientStarter()
    {
    }

    public String start()
    {
        return "swing";
    }
}
