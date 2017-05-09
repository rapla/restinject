package org.rapla.scheduler;

import org.rapla.function.Command;
import org.rapla.function.Consumer;

public interface CommandScheduler
{
    Cancelable schedule(Command command, long delay);
    Cancelable schedule(Command command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/
    Cancelable scheduleSynchronized(final Object synchronizationObject, Runnable task, final long delay);

    Promise<Void> run(Command supplier);
    <T> Promise<T> supply(Callable<T> supplier);
    <T> Promise<T> supplyProxy(Callable<T> supplier);

    <T> Promise<T> synchronizeTo(Promise<T> promise);
    <T> CompletablePromise<T> createCompletable();

    @FunctionalInterface
    interface Callable<T> {
        T call() throws Exception;
    }

}
