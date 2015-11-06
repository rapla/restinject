package org.rapla.inject.component.gwt;

import javax.inject.Inject;

import org.rapla.inject.ComponentStarter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context=InjectionContext.gwt, of=ComponentStarter.class)
public class GwtStarter implements ComponentStarter
{

    @Inject
    public GwtStarter()
    {
    }
}
