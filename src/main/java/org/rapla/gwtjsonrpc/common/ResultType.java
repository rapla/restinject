package org.rapla.gwtjsonrpc.common;

/*
* not needed anymore
 */
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(value={java.lang.annotation.ElementType.METHOD})
@Deprecated
public @interface ResultType {

	Class value();
	Class container() default Object.class;
}
