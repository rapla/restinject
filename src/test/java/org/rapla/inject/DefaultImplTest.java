package org.rapla.inject;

@DefaultImplementationRepeatable({
@DefaultImplementation(of=ImplInterface.class,context = InjectionContext.all),
@DefaultImplementation(of=OtherInterface.class,context = InjectionContext.all)
})
public class DefaultImplTest implements  ImplInterface, OtherInterface
{
}
