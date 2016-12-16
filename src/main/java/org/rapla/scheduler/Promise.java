package org.rapla.scheduler;

import org.rapla.function.BiConsumer;
import org.rapla.function.BiFunction;
import org.rapla.function.Command;
import org.rapla.function.Consumer;

/** same as @CompletableFuture but usable in gwt */
public interface Promise<T>
{
    Void VOID = null;

    <U> Promise<U> thenApply(org.rapla.function.Function<? super T, ? extends U> fn);

    Promise<Void> thenAccept(Consumer<? super T> fn);

    Promise<Void> thenRun(Command fn);

    <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn);

    Promise<Void> runAfterBoth(Promise<?> other, Command fn);

    <U> Promise<U> applyToEither(Promise<? extends T> other, org.rapla.function.Function<? super T, U> fn);

    Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn);

    Promise<Void> runAfterEither(Promise<?> other, Command fn);

    <U> Promise<U> thenCompose(org.rapla.function.Function<? super T, ? extends Promise<U>> fn);

    Promise<T> exceptionally(org.rapla.function.Function<Throwable, ? extends T> fn);

    Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn);

    <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);
}

