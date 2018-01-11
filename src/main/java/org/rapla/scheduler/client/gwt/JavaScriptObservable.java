package org.rapla.scheduler.client.gwt;

import com.github.timofeevda.gwt.rxjs.interop.functions.Action1;
import com.github.timofeevda.gwt.rxjs.interop.functions.Func1;
import com.github.timofeevda.gwt.rxjs.interop.observable.Observable;
import com.github.timofeevda.gwt.rxjs.interop.observable.Observer;
import com.github.timofeevda.gwt.rxjs.interop.subject.Subject;
import com.github.timofeevda.gwt.rxjs.interop.subscription.Subscription;
import com.google.gwt.core.client.JavaScriptException;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.sync.JavaObservable;
import org.reactivestreams.Publisher;

public class JavaScriptObservable<T> implements org.rapla.scheduler.Observable<T>
{
    protected Observable observable;

    JavaScriptObservable(Observable observable)
    {
        this.observable = observable;
    }

    JavaScriptObservable(Promise<T> promise)
    {
        Subject<T> subject = new Subject<T>();
        promise.thenAccept( (t) ->{
            subject.next( t); subject.complete();
        });
        promise.exceptionally( (ex) -> { subject.error( ex);return null; });
        this.observable = subject;
    }

    @Override
    public Disposable subscribe() {
        Action1 action1 = (t) ->
        {
        };
        final Subscription subscription = observable.subscribe(action1);
        return subscribtionToDisposable( subscription);
    }

    @Override
    public Disposable subscribe(Consumer<? super T> consumer)
    {
        Action1 action1 = (t) ->
        {
            try
            {
                consumer.accept((T) t);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        };
        final Subscription subscription = observable.subscribe(action1);
        return subscribtionToDisposable(subscription);
    }

    protected Disposable subscribtionToDisposable(Subscription subscription) {
        return new Disposable()
        {
            @Override
            public void dispose()
            {
                subscription.unsubscribe();
            }

            @Override
            public boolean isDisposed()
            {
                return subscription.isClosed();
            }
        };
    }

    @Override
    public org.rapla.scheduler.Observable<T> throttle(long milliseconds)
    {
        final int milliseconds1 = (int) milliseconds;
        final Observable observable = this.observable.throttleTime(milliseconds1);
        return t(observable);
    }

    @Override
    public org.rapla.scheduler.Observable<T> delay(long milliseconds) {
        final int milliseconds1 = (int) milliseconds;
        final Observable observable = this.observable.delay(milliseconds1);
        return t(observable);
    }

    @Override
    public org.rapla.scheduler.Observable<T> repeat() {
        final Observable observable = this.observable.repeat();
        return t(observable);
    }

    @Override
    public org.rapla.scheduler.Observable<T> concatWith(org.rapla.scheduler.Observable<? extends T> otherObservable)
    {
        final Observable observable = ((JavaScriptObservable) otherObservable).observable;
        final Observable concat = this.observable.concat(observable);
        return t(concat) ;
    }

    @Override
    public org.rapla.scheduler.Observable<T> doOnError(Consumer<? super Throwable> onError)
    {
        throw new UnsupportedOperationException();
    }

    private <R> org.rapla.scheduler.Observable<R> t(Observable observable)
    {
        return new JavaScriptObservable<R>(observable);
    }

    @Override
    public <R> org.rapla.scheduler.Observable<R> map(Function<? super T, ? extends R> mapper)
    {
        Func1<T,R> mapper2 = ( arg)->
        {
            final R apply;
            try
            {
                apply = mapper.apply(arg);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            return apply;
        };
        final Observable<R> map = observable.map(mapper2);
        final org.rapla.scheduler.Observable<R> t = t(observable);
        return t;
    }

    @Override
    public <R> org.rapla.scheduler.Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        Observable.Projector<T, R> projector = getMapProjector(mapper);
        final Observable observable = this.observable.mergeMap(projector);
        return new JavaScriptObservable<R>(observable);
    }



    @Override
    public <R> org.rapla.scheduler.Observable<R> switchMap(Function<? super T,  ? extends Publisher<? extends R>> mapper)
    {
        Observable.Projector<T, R> projector = getMapProjector(mapper);
        final Observable observable = this.observable.switchMap(projector);
        return new JavaScriptObservable<R>(observable);
    }

    protected <R> Observable.Projector<T, R> getMapProjector(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return new Observable.Projector<T,R>()
            {
                @Override
                public Observable<R> project(T item, int index)
                {
                    try
                    {
                        Publisher<? extends R> apply = mapper.apply(item);
                        Observable<R> jsObservable = ((JavaScriptObservable<R>) apply).observable;
                        return jsObservable;
                    } catch (Exception ex)
                    {
                        return Observable._throw( ex);
                    }
                }
            };
    }

    @Override
    public void subscribe(org.reactivestreams.Subscriber<? super T> observer)
    {
        Observer<T> jsObserver  = new Observer<T>()
        {
            @Override
            public void error(Object error)
            {
                if ( error instanceof JavaScriptException)
                {
                    final JavaScriptException jsError = (JavaScriptException) error;
                    final Throwable cause = jsError.getCause();
                    if ( cause != null)
                    {
                        observer.onError(cause);
                    }
                    else
                    {
                        observer.onError( jsError );
                    }
                }
            }

            @Override
            public void next(T value)
            {
                observer.onNext( value);
            }

            @Override
            public void complete()
            {
                observer.onComplete();
            }
        };
        observable.subscribe( jsObserver );
    }

    @Override
    public Observable toNativeObservable()
    {
        return observable;
    }

    @Override
    public org.rapla.scheduler.Observable<T> debounce(long time) {
        final Observable observable = this.observable.debounceTime((int) time);
        return t(observable);
    }
}

