package org.rapla.scheduler;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** same as @CompletableFuture but usable in gwt */
public interface Promise<T>
{
    Void VOID = null;

    <U> Promise<U> thenApply(Function<? super T, ? extends U> fn);

    Promise<Void> thenAccept(Consumer<? super T> fn);

    Promise<Void> thenRun(Runnable fn);

    <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn);

    Promise<Void> runAfterBoth(Promise<?> other, Runnable fn);

    <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn);

    Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn);

    Promise<Void> runAfterEither(Promise<?> other, Runnable fn);

    <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn);

    Promise<T> exceptionally(Function<Throwable, ? extends T> fn);

    Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn);

    <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    @FunctionalInterface
    public interface Function<T, R>
    {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Consumer<F>
    {
        void accept(F t) throws Exception;
    }

    @FunctionalInterface
    public interface BiFunction<T, U, R>
    {
        R apply(T t, U u) throws Exception;
    }

    @FunctionalInterface
    public interface BiConsumer<T, U>
    {
        void accept(T t, U u) throws Exception;
    }

}

