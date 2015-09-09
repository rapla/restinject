package org.rapla.inject;

public @interface ExtensionPoint
{
    InjectionContext[] context() default {};
}
