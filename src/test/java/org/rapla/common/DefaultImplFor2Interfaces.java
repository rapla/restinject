package org.rapla.common;

import javax.inject.Inject;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(of=ImplInterface.class,context = InjectionContext.all)
@DefaultImplementation(of=OtherInterface.class,context = InjectionContext.server)
public class DefaultImplFor2Interfaces implements  ImplInterface, OtherInterface
{
    @Inject
    public DefaultImplFor2Interfaces()
    {

    }
}
