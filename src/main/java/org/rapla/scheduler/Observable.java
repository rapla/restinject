package org.rapla.scheduler;

import io.reactivex.Flowable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.SingleSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;
import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

@JsType
public interface Observable<T> extends Publisher<T> {
    /**
     * @see Flowable#subscribe()
     */
    Disposable subscribe();
    /**
     * @see Flowable#subscribe(Consumer)
     */
    @JsMethod(name = "subscribeWithConsumer")
    Disposable subscribe(Consumer<? super T> consumer);
    /**
     * @see Flowable#throttleLast(long, TimeUnit)
     */
    Observable<T> throttle(long milliseconds);
    /**
     * @see Flowable#delay(long, TimeUnit) ()
     */
    Observable<T> delay(long milliseconds);
    /**
     * @see Flowable#repeat()
     */
    Observable<T> repeat();
    /**
     * @see Flowable#concatWith(Publisher)
     */    Observable<T> concatWith(Observable<? extends T> otherObservable);
    Observable<T> doOnError(Consumer<? super Throwable> onError);
    /**
     * @see Flowable#map(Function)
     */
    <R> Observable<R> map(Function<? super T, ? extends R> mapper);
    /**
     * @see Flowable#flatMap(Function)
     */
    <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper);
    /**
     * @see Flowable#switchMap(Function)
     */
    <R> Observable<R> switchMap(Function<? super T, ? extends Publisher<? extends R>> mapper);
    /**
     * @see Flowable#debounce(long, TimeUnit)
     */
    Observable<T> debounce(long time);
    Object toNativeObservable();
}
