package org.rapla.rest.client.gwt.internal.impl.ser;

import javax.inject.Provider;

public class SimpleGwtProvider<T> implements Provider<T> {
    T t;
    public SimpleGwtProvider(T t) {
        this.t = t;
    }

    public T get()
    {
        return t;
    }
   
}
