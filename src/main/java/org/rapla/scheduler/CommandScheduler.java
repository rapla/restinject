package org.rapla.scheduler;

public interface CommandScheduler
{
    Cancelable schedule(Command command, long delay);
    Cancelable schedule(Command command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/
    Cancelable scheduleSynchronized(final Object synchronizationObject, Command task, final long delay);

    Promise<Void> run(Command supplier);
    <T> Promise<T> supply(Callable<T> supplier);
    <T> Promise<T> supplyProxy(Callable<T> supplier);

    @FunctionalInterface
    interface Callable<T> {
        T call() throws Exception;
    }

}
