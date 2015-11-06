package org.rapla.inject.component.gwt;

import javax.inject.Inject;

import org.rapla.common.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context=InjectionContext.gwt, of=ComponentStarter.class,export = true)
public class GwtStarter implements ComponentStarter
{

    @Inject
    public GwtStarter()
    {
    }

    public String start()
    {
        return "gwt";
    }
}
