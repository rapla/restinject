package org.rapla.scheduler;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

public interface CommandScheduler
{
    Cancelable schedule(Command command, long delay);
    Cancelable schedule(Command command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/

    <T> Promise<T> supply(Supplier<T> supplier);
    Promise<Void> run(Runnable supplier);

    // TODO GWT implmentation
    <T> Promise<T> supplyProxy(Supplier<T> supplier);

    @FunctionalInterface
    public interface Supplier<T> {
        T get();
    }

}

class Test
{
    interface  ProxyTest
    {
        public String doSomething();
    }
    public static void main(String[] args)
    {

        CommandScheduler manager = null;
        manager.schedule(() -> {return;}, 0);
        ProxyTest proxyTest = null;
        manager.supplyProxy(() ->  proxyTest.doSomething()).thenRun(()->{System.out.println("Done");});
        manager.supply(() -> {return "test";});
        manager.run(() -> {System.out.println("Test");});
    }
}