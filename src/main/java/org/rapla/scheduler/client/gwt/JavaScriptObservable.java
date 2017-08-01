package org.rapla.scheduler.client.gwt;

import com.github.timofeevda.gwt.rxjs.interop.functions.Action1;
import com.github.timofeevda.gwt.rxjs.interop.functions.Func1;
import com.github.timofeevda.gwt.rxjs.interop.observable.Observable;
import com.github.timofeevda.gwt.rxjs.interop.subscription.Subscription;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.util.stream.Stream;

public class JavaScriptObservable<T> implements org.rapla.scheduler.Observable<T>
{
    Observable observable;

    JavaScriptObservable(Observable observable)
    {
        this.observable = observable;
        Observable.merge(observable, observable);
        final Observable observable1 = observable.throttleTime(250);
        Observable.Projector test = new Observable.Projector()
        {
            @Override
            public Observable project(Object item, int index)
            {
                return null;
            }
        };
        Observable.ResultSelector test2 = new Observable.ResultSelector()
        {
            @Override
            public Object selectResult(Object outerValue, Object innerValue, int outerIndex, int innerIndex)
            {
                return null;
            }
        };
        observable1.switchMap(test);
        Stream<String> stream = null;
        final Stream<Integer> integerStream = stream.map((string) -> string.length());

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
    public <R> org.rapla.scheduler.Observable<R> switchMap(Function<? super T, ? extends ObservableSource<? extends R>> mapper)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void subscribe(io.reactivex.Observer<? super T> observer)
    {

    }

}

