package org.rapla.scheduler;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

public interface CommandScheduler
{
    Cancelable schedule(Command command, long delay);
    Cancelable schedule(Command command, long delay, long period);
    /** if two commands are scheduled for the same synchronisation object then they must be executed in the order in which they are scheduled*/

    <U> Promise<U> supply(Supplier<U> supplier);
    Promise<Void> run(Runnable supplier);

    // TODO GWT implmentation
    <T,U> Promise<U> supplyProxy(T t,ProxyPromiseOperation<U,T> supplier);

    public interface ProxyPromiseOperation<T,S>
    {
        T get(S t) throws Exception;
    }

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
        manager.supplyProxy(proxyTest,(proxy) ->  proxy.doSomething()).thenRun(()->{System.out.println("Done");});
        manager.supply(() -> {return "test";});
        manager.run(() -> {System.out.println("Test");});
    }
}