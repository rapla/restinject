package org.rapla.inject.extension;

import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

@ExtensionPoint(id="org.rapla.inject.test",context = InjectionContext.all)
public interface InjectionTestInterface
{
}
