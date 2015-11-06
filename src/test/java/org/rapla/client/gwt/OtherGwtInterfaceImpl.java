package org.rapla.client.gwt;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.common.OtherInterface;

import javax.inject.Inject;

@DefaultImplementation(of = OtherInterface.class, context = InjectionContext.gwt)
public class OtherGwtInterfaceImpl implements OtherInterface
{
    @Inject
    public OtherGwtInterfaceImpl()
    {
    }
}
