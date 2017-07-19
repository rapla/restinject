package org.rapla.scheduler.client.gwt;

import com.github.timofeevda.gwt.rxjs.interop.observable.Observable;
import com.github.timofeevda.gwt.rxjs.interop.observable.Subscriber;
import com.github.timofeevda.gwt.rxjs.interop.subscription.Subscription;

import java.util.stream.Stream;

public class JavaScriptObservable
{
    JavaScriptObservable()
    {
        Observable observable = null;
        Observable.merge( observable, observable);
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
        observable1.switchMap( test);
        Stream<String> stream=null;
        final Stream<Integer> integerStream = stream.map((string) -> string.length());

    }
}
