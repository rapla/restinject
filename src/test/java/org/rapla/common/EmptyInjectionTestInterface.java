package org.rapla.common;

import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

@ExtensionPoint(id="org.rapla.inject.test",context = InjectionContext.all)
public interface EmptyInjectionTestInterface
{
}
