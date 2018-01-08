package org.rapla.scheduler.sync;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.rapla.scheduler.Observable;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class JavaObservable<T> implements Observable<T>
{
    protected io.reactivex.Observable<T> observable;
    Scheduler scheduler =null;

    public JavaObservable(SynchronizedPromise<T> promise, Executor executor)
    {
        this(io.reactivex.Observable.fromFuture(promise.toFuture()), executor);
    }


    public JavaObservable(io.reactivex.Observable<T> observable, Executor executor)
    {
        this(observable, Schedulers.from( executor));
    }

    public JavaObservable(io.reactivex.Observable<T> observable, Scheduler scheduler)
    {
        this.observable = observable;
        this.scheduler = scheduler;
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

        final io.reactivex.Observable<T> tFlowable = observable.throttleLast(milliseconds, TimeUnit.MILLISECONDS, scheduler);
        return t(tFlowable);
    }

    private <R> Observable<R> t(final io.reactivex.Observable<R> tFlowable)
    {
        return new JavaObservable<>(tFlowable, scheduler);
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

    @Override
    public Observable<T> debounce(long time) {
        final io.reactivex.Observable<T> debounce = observable.debounce(time, TimeUnit.MILLISECONDS, scheduler);
        return t(debounce);
    }
}
