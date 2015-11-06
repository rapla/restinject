package org.rapla.inject;

import javax.inject.Inject;

@DefaultImplementation(of = OtherInterface.class, context = InjectionContext.gwt)
public class OtherGwtInterfaceImpl implements OtherInterface
{
    @Inject
    public OtherGwtInterfaceImpl()
    {
    }
}
