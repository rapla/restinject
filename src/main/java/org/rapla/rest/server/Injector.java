package org.rapla.rest.server;

public interface Injector<T>
{
    void injectMembers(T instance) throws Exception;
}
