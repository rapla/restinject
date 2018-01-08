package org.rapla.scheduler;

import io.reactivex.functions.Action;

import java.util.concurrent.TimeUnit;

public interface CommandScheduler
{
    //Cancelable schedule(Action command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/
    Promise<Void> scheduleSynchronized(final Object synchronizationObject, Action task);
    Promise<Void> delay(final long delay);
    Observable<Long> intervall(long initialDelay,long periodMilliseconds);
    Promise<Void> run(Action task);
    <T> Promise<T> supply(Callable<T> supplier);
    <T> Subject<T> createPublisher();
    <T> CompletablePromise<T> createCompletable();
    //<T> Promise<T> synchronizeTo(Promise<T> promise);
    //    <T> T waitFor(Promise<T> promise, int timeout) throws Exception;
    <T> Observable<T> toObservable(Promise<T> promise);

    @FunctionalInterface
    interface Callable<T> {
        T call() throws Exception;
    }

}
