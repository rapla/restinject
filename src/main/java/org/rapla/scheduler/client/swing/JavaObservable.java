package org.rapla.scheduler.client.swing;

import io.reactivex.ObservableSource;
import org.rapla.function.Command;
import org.rapla.function.Consumer;
import org.rapla.function.Function;
import org.rapla.scheduler.Observable;

import java.util.concurrent.TimeUnit;

public class JavaObservable<T> implements Observable<T>
{
    io.reactivex.Observable<T> observable;
    public JavaObservable(io.reactivex.Observable<T> observable)
    {
        this.observable = observable;
        io.reactivex.functions.Function<T, ObservableSource<Integer>> mapper = (test) ->{
            String test4 = test.toString();
            ObservableSource<Integer> bla = io.reactivex.Observable.just( test4.length());
            return bla;
        };
        int sd3 = 1;
        final io.reactivex.Observable<Integer> integerObservable = observable.switchMap(mapper, sd3);
        observable.throttleLast(500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<T> subscribe()
    {
        return null;
    }
}
