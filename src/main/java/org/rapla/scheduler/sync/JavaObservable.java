package org.rapla.scheduler.sync;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import org.rapla.scheduler.Observable;

import java.util.concurrent.TimeUnit;

public class JavaObservable<T> implements Observable<T>
{
    io.reactivex.Observable<T> observable;

    public JavaObservable(SynchronizedPromise<T> promise)
    {
        this(io.reactivex.Observable.fromFuture(promise.toFuture()));
    }


    public JavaObservable(io.reactivex.Observable<T> observable)
    {
        this.observable = observable;
    }


    @Override
    public Observable<T> doOnError(Consumer<? super Throwable> onError)
    {
        final io.reactivex.Observable<T> tFlowable = observable.doOnError(onError);
        return t(tFlowable);
    }



    @Override
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper)
    {
        final io.reactivex.Observable<R> tFlowable = observable.map(mapper);
        return t(tFlowable);
    }

    @Override
    public Disposable subscribe(Consumer<? super T> consumer)
    {
        return observable.subscribe( consumer);
    }

    @Override
    public Observable<T> throttle(long milliseconds)
    {
        final io.reactivex.Observable<T> tFlowable = observable.throttleLast(milliseconds, TimeUnit.MILLISECONDS);
        return t(tFlowable);
    }

    private <R> Observable<R> t(final io.reactivex.Observable<R> tFlowable)
    {
        return new JavaObservable<>(tFlowable);
    }

    @Override
    public <R> Observable<R> switchMap(Function<? super T, Observable<R>> mapper)
    {
        final io.reactivex.Observable<R> rFlowable = observable.switchMap(mapper);
        return t(rFlowable);
    }

    @Override
    public void subscribe(Observer<? super T> observer)
    {
        observable.subscribe(observer);
    }

    @Override
    public io.reactivex.Observable<T> toNativeObservable()
    {
        return observable;
    }
}
