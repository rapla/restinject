package org.rapla.function;

@FunctionalInterface
public interface Consumer<F>
{
    void accept(F t) throws Exception;
}
