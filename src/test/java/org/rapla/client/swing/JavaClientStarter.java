package org.rapla.client.swing;

import org.rapla.common.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import dagger.MembersInjector;

import java.util.Map;

import javax.inject.Inject;

@DefaultImplementation(context = InjectionContext.swing, of = ComponentStarter.class, export = true)
public class JavaClientStarter implements ComponentStarter
{
    @Inject
    public JavaClientStarter()
    {
    }

    @Override
    public Map<String, MembersInjector> getMembersInjector()
    {
        return null;
    }

    public String start()
    {
        return "swing";
    }
}
