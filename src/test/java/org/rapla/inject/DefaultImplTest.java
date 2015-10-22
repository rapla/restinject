package org.rapla.inject;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@DefaultImplementationRepeatable({
@DefaultImplementation(of=ImplInterface.class,context = InjectionContext.all),
@DefaultImplementation(of=OtherInterface.class,context = InjectionContext.all)
})
public class DefaultImplTest implements  ImplInterface, OtherInterface
{
    @Inject
    public DefaultImplTest()
    {

    }
}
