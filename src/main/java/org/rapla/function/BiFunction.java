package org.rapla.function;

@FunctionalInterface
public interface BiFunction<T, U, R>
{
    R apply(T t, U u) throws Exception;
}
