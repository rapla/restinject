package org.rapla.scheduler;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import jsinterop.annotations.JsType;

@JsType
public interface Observable<T> extends ObservableSource<T> {
    Disposable subscribe(Consumer<? super T> consumer);
    Observable<T> throttle(long milliseconds);
    Observable<T> doOnError(Consumer<? super Throwable> onError);
    <R> Observable<R> map(Function<? super T, ? extends R> mapper);
    <R> Observable<R> switchMap(Function<? super T, Observable<R>> mapper);
    Object toNativeObservable();
    Observable<T> debounce(long time);
}
