package org.rapla.scheduler;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

public interface CommandScheduler
{
    Cancelable schedule(Command command, long delay);
    Cancelable schedule(Command command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/
    Promise<Void> run(Command supplier);
    <T> Promise<T> supply(Callable<T> supplier);
    // TODO GWT implmentation
    <T> Promise<T> supplyProxy(Callable<T> supplier);

    @FunctionalInterface
    public interface Callable<T> {
        T call() throws Exception;
    }

}
