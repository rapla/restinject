package org.rapla.gwtjsonrpc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteJsonMethod
{
    String path() default "";
}