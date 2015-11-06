package org.rapla.inject;

import javax.inject.Inject;

@DefaultImplementationRepeatable({
@DefaultImplementation(of=ImplInterface.class,context = InjectionContext.all),
@DefaultImplementation(of=OtherInterface.class,context = InjectionContext.server)
})
public class DefaultImplFor2Interfaces implements  ImplInterface, OtherInterface
{
    @Inject
    public DefaultImplFor2Interfaces()
    {

    }
}
