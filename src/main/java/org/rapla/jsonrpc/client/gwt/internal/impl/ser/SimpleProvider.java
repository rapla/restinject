package org.rapla.jsonrpc.client.gwt.internal.impl.ser;

import javax.inject.Provider;

public class SimpleProvider<T> implements Provider<T> {
    T t;
    public SimpleProvider(T t) {
        this.t = t;
    }
    
    public T get()
    {
        return t;
    }
   
}
