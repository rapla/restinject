package org.rapla.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Christopher on 09.09.2015.
 */
@Target(value={ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DefaultImplementationRepeatable.class)
public @interface DefaultImplementation
{
    Class of();
    InjectionContext[] context() default {};
}
