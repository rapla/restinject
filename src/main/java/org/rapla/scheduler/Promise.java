package org.rapla.scheduler;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/** same as @CompletableFuture but usable in gwt */
public interface Promise<T>
{
    <U> Promise<U> thenApply(Function<? super T, ? extends U> fn);

    Promise<Void> thenAccept(Consumer<? super T> action);

    Promise<Void> thenRun(Runnable action);

    <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> action);

    Promise<Void> runAfterBoth(Promise<?> other, Runnable action);

    <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn);

    Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> action);

    Promise<Void> runAfterEither(Promise<?> other, Runnable action);

    <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn);

    Promise<T> exceptionally(Function<Throwable, ? extends T> fn);

    Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    @FunctionalInterface
    public interface Function<T, R>
    {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Consumer<F>
    {
        void accept(F t);
    }

    @FunctionalInterface
    public interface BiFunction<T, U, R>
    {
        R apply(T t, U u);
    }

    @FunctionalInterface
    public interface BiConsumer<T, U>
    {
        void accept(T t, U u);
    }

}

