package org.rapla.scheduler.sync;

import io.reactivex.disposables.Disposable;
import org.rapla.scheduler.Subject;

import java.util.concurrent.Executor;


public class JavaSubject<T> extends  JavaObservable<T> implements Subject<T> {

    public JavaSubject(io.reactivex.subjects.Subject<T> observable, Executor executor) {
        super(observable, executor);
    }

    @Override
    public void onSubscribe(Disposable d) {
        ((io.reactivex.subjects.Subject<T>)observable).onSubscribe(d);
    }

    @Override
    public void onNext(T t) {
        ((io.reactivex.subjects.Subject<T>)observable).onNext(t);
    }

    @Override
    public void onError(Throwable e) {
        ((io.reactivex.subjects.Subject<T>)observable).onError(e);
    }

    @Override
    public void onComplete() {
        ((io.reactivex.subjects.Subject<T>)observable).onComplete();
    }
}
