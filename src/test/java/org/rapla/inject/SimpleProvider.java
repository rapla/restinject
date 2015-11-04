package org.rapla.inject;

import javax.inject.Provider;

public class SimpleProvider<T> implements Provider<T> {
    T t;
    public SimpleProvider(T t) {
        this.t = t;
    }
    public SimpleProvider(Provider<T> t) {
        this.t = t.get();
    }


    public T get()
    {
        return t;
    }
   
}
