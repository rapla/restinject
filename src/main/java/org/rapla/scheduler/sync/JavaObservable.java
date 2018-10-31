package org.rapla.scheduler.sync;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.rapla.scheduler.Observable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class JavaObservable<T> implements Observable<T>
{
    protected io.reactivex.Flowable<T> observable;
    Scheduler scheduler =null;

    public JavaObservable(SynchronizedPromise<T> promise, Executor executor)
    {
        this(toObservable(promise.toFuture()), executor);
    }

    private static <T> io.reactivex.Flowable<T> toObservable(CompletableFuture<T> future) {
        BackpressureStrategy strategy = BackpressureStrategy.BUFFER;
        return io.reactivex.Flowable.create(subscriber ->
                future.whenComplete((result, error) -> {
                    if (error != null) {
                        subscriber.onError(error);
                    } else {
                        if ( result != null) {
                            subscriber.onNext(result);
                        }
                        subscriber.onComplete();
                    }
                }), strategy);
    }

    public JavaObservable(io.reactivex.Flowable<T> observable, Executor executor)
    {
        this(observable, Schedulers.from( executor));
    }

    public JavaObservable(io.reactivex.Flowable<T> observable, Scheduler scheduler)
    {
        this.observable = observable;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<T> doOnError(Consumer<? super Throwable> onError)
    {
        final io.reactivex.Flowable<T> tFlowable = observable.doOnError(onError);
        return t(tFlowable);
    }

    @Override
    public Observable<T> onErrorResumeNext(Consumer<? super Throwable> onError)
    {
        Function<? super Throwable, ? extends Publisher<T>> handler = (ex)->
        {
            onError.accept( ex);
            return this;
        };
        final io.reactivex.Flowable<T> tFlowable = observable.onErrorResumeNext(handler);
        return t(tFlowable);
    }


    @Override
    public Observable<T> doOnNext(Consumer<? super T> next)
    {
        final io.reactivex.Flowable<T> tFlowable = observable.doOnNext(next);
        return t(tFlowable);
    }

    @Override
    public Observable<T> doOnComplete(Action action)
    {
        final io.reactivex.Flowable<T> tFlowable = observable.doOnComplete(action);
        return t(tFlowable);
    }


    @Override
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper)
    {
        final io.reactivex.Flowable<R> tFlowable = observable.map(mapper);
        return t(tFlowable);
    }

    @Override
    public Disposable subscribe() {
        return observable.subscribe();
    }

    @Override
    public Disposable subscribe(Consumer<? super T> consumer)
    {
        return observable.subscribe( consumer);
    }

    @Override
    public Observable<T> throttle(long milliseconds)
    {
        final io.reactivex.Flowable<T> tFlowable = observable.throttleLast(milliseconds, TimeUnit.MILLISECONDS, scheduler);
        return t(tFlowable);
    }

    @Override
    public Observable<T> delay(long milliseconds) {
        final io.reactivex.Flowable<T> tFlowable = observable.delay(milliseconds,TimeUnit.MILLISECONDS);
        return t(tFlowable);
    }

    @Override
    public Observable<T> repeat() {
        final io.reactivex.Flowable<T> tFlowable = observable.repeat();
        return t(tFlowable);
    }

    @Override
    public Observable<T> concatWith(Observable<? extends T> otherObservable) {
        final io.reactivex.Flowable<T> tFlowable = observable.concatWith(otherObservable);
        return t(tFlowable);
    }

    private <R> Observable<R> t(final io.reactivex.Flowable<R> tFlowable)
    {
        return new JavaObservable<>(tFlowable, scheduler);
    }

    @Override
    public <R> Observable<R> switchMap(Function<? super T, ? extends Publisher<? extends R>> mapper)
    {
        final io.reactivex.Flowable<R> rFlowable = observable.switchMap(mapper);
        return t(rFlowable);
    }


    @Override
    public <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper)
    {
        final io.reactivex.Flowable<R> rFlowable = observable.flatMap(mapper);
        return t(rFlowable);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber)
    {
        observable.subscribe(subscriber);
    }

    @Override
    public io.reactivex.Flowable<T> toNativeObservable()
    {
        return observable;
    }

    @Override
    public Observable<T> debounce(long time) {
        final io.reactivex.Flowable<T> debounce = observable.debounce(time, TimeUnit.MILLISECONDS, scheduler);
        return t(debounce);
    }
}
