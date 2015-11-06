package org.rapla.inject.gwt;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.OtherInterface;

import javax.inject.Inject;

@DefaultImplementation(of = OtherInterface.class, context = InjectionContext.gwt)
public class OtherGwtInterfaceImpl implements OtherInterface
{
    @Inject
    public OtherGwtInterfaceImpl()
    {
    }
}
