package org.rapla.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Christopher on 09.09.2015.
 */
@Target(value={ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultImplementationRepeatable
{
    DefaultImplementation[] value();
}
