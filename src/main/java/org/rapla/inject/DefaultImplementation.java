package org.rapla.inject;

/**
 * Created by Christopher on 09.09.2015.
 */
public @interface DefaultImplementation
{
    Class of();
    InjectionContext[] context() default {};
}
