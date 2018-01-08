package org.rapla.scheduler.client.gwt;

import com.github.timofeevda.gwt.rxjs.interop.functions.Action1;
import com.github.timofeevda.gwt.rxjs.interop.observable.OnSubscribe;
import com.github.timofeevda.gwt.rxjs.interop.observable.Subscriber;
import com.github.timofeevda.gwt.rxjs.interop.subscription.TearDownSubscription;
import io.reactivex.functions.Action;
import org.rapla.logger.Logger;
import org.rapla.scheduler.*;

import java.util.concurrent.TimeUnit;

public class GwtCommandScheduler implements CommandScheduler {
    protected Logger logger;

    public GwtCommandScheduler(Logger logger) {
        this.logger = logger;
    }

    public Promise<Void> schedule(final Action command) {
        final CompletablePromise<Void> completable = createCompletable();
        SchedulerImpl.ScheduledCommand entry = new SchedulerImpl.ScheduledCommand() {

            @Override
            public void execute() {
                try {
                    //gwtLogger.info("Refreshing client without period ");
                    command.run();
                    completable.complete(null);
                } catch (Exception e) {
                    warn(e.getMessage(), e);
                    completable.completeExceptionally(e);
                }

            }
        };
        SchedulerImpl.get(logger).scheduleDeferred(entry);
        return completable;
    }


    @Override
    public Promise<Void> delay(long periodLengthInMilliseconds) {
        final int periodLengthInMilliseconds1 = (int) periodLengthInMilliseconds;
        final com.github.timofeevda.gwt.rxjs.interop.observable.Observable<Integer> interval = com.github.timofeevda.gwt.rxjs.interop.observable.Observable.timer(periodLengthInMilliseconds1);
        CompletablePromise<Void> promise = createCompletable();
        final Action1 action1 = (time) -> promise.complete(null);
        interval.subscribe(action1);
        return promise;
    }

    @Override
    public Observable<Long> intervall( long initialDelay,long periodMilliseconds) {
        com.github.timofeevda.gwt.rxjs.interop.observable.Observable<Integer> interval = com.github.timofeevda.gwt.rxjs.interop.observable.Observable.interval((int)periodMilliseconds);
        if ( initialDelay>0)
        {
            interval = interval.delay( (int)initialDelay);
        }
        final com.github.timofeevda.gwt.rxjs.interop.observable.Observable<Long> map = interval.map((intValue) -> Integer.toUnsignedLong(intValue));
        return new JavaScriptObservable<Long>(map);
    }

    @Override
    public <T> Promise<T> supply(final Callable<T> supplier) {
        final UnsynchronizedCompletablePromise<T> promise = new UnsynchronizedCompletablePromise<T>();
        scheduleDeferred(() ->
        {
            try {
                final T result = supplier.call();
                promise.complete(result);
            } catch (Throwable ex) {
                promise.completeExceptionally(ex);
            }
        });
        return promise;
    }

    @Override
    public Promise<Void> run(final Action supplier) {
        final UnsynchronizedCompletablePromise<Void> promise = new UnsynchronizedCompletablePromise<Void>();
        scheduleDeferred(() ->
        {
            try {
                supplier.run();
                promise.complete(null);
            } catch (Throwable ex) {
                promise.completeExceptionally(ex);
            }
        });
        return promise;
    }

    private void scheduleDeferred(SchedulerImpl.ScheduledCommand cmd) {
        SchedulerImpl.get(logger).scheduleDeferred(cmd);
    }


    @Override
    public Promise<Void> scheduleSynchronized(Object synchronizationObject, Action task) {
        return schedule(() -> task.run());
    }

    protected void warn(String message, Exception e) {
        logger.warn(message, e);
    }

    @Override
    public <T> CompletablePromise<T> createCompletable() {
        return new UnsynchronizedCompletablePromise<>();
    }

    @Override
    public <T> Observable<T> toObservable(Promise<T> promise) {
        return new JavaScriptObservable<>(promise);
    }

    @Override
    public <T> Subject<T> createPublisher() {
        com.github.timofeevda.gwt.rxjs.interop.subject.Subject subject = new com.github.timofeevda.gwt.rxjs.interop.subject.Subject();
        return new JavaScriptSubject<T>(subject);
    }
}
