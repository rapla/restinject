package org.rapla.common.extension;

import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

@ExtensionPoint(context=InjectionContext.all, id="org.rapla.Example")
public interface ExampleExtensionPoint
{
    void doSomething();
}
