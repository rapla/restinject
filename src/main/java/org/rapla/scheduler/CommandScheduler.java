package org.rapla.scheduler;

import io.reactivex.functions.Action;

public interface CommandScheduler
{
    Cancelable schedule(Action command, long delay);
    Cancelable schedule(Action command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/
    Cancelable scheduleSynchronized(final Object synchronizationObject, Runnable task, final long delay);

    Promise<Void> run(Action supplier);
    <T> Promise<T> supply(Callable<T> supplier);
    <T> CompletablePromise<T> createCompletable();
    //<T> Promise<T> synchronizeTo(Promise<T> promise);
    //    <T> T waitFor(Promise<T> promise, int timeout) throws Exception;

    <T> Observable<T> toObservable(Promise<T> promise);

    @FunctionalInterface
    interface Callable<T> {
        T call() throws Exception;
    }

}
