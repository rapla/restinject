package org.rapla.scheduler.client.gwt;

import com.github.timofeevda.gwt.rxjs.interop.observable.Observable;
import io.reactivex.disposables.Disposable;
import org.rapla.scheduler.Subject;
import org.reactivestreams.Subscription;

public class JavaScriptSubject<T> extends JavaScriptObservable<T> implements Subject<T> {

    JavaScriptSubject(com.github.timofeevda.gwt.rxjs.interop.subject.Subject observable) {
        super(observable);
    }


    @Override
    public void onSubscribe(Subscription subscription) {
        throw new IllegalStateException("onSubscribe not handled by JS-Subject");
    }

    @Override
    public void onNext(T t) {
        ((com.github.timofeevda.gwt.rxjs.interop.subject.Subject<T>)observable).next( t);
    }

    @Override
    public void onError(Throwable e) {
        ((com.github.timofeevda.gwt.rxjs.interop.subject.Subject<T>)observable).error( e);
    }

    @Override
    public void onComplete() {
        ((com.github.timofeevda.gwt.rxjs.interop.subject.Subject<T>)observable).complete( );
    }
}
