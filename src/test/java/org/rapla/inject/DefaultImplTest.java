package org.rapla.inject;

import javax.inject.Inject;

@DefaultImplementationRepeatable({
@DefaultImplementation(of=ImplInterface.class,context = InjectionContext.all),
@DefaultImplementation(of=OtherInterface.class,context = InjectionContext.server)
})
public class DefaultImplTest implements  ImplInterface, OtherInterface
{
    @Inject
    public DefaultImplTest()
    {

    }
}
