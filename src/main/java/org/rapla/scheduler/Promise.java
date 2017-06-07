package org.rapla.scheduler;

import org.rapla.function.BiConsumer;
import org.rapla.function.BiFunction;
import org.rapla.function.Command;
import org.rapla.function.Consumer;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/** same as {@link java.util.concurrent.CompletionStage} but usable in gwt */
public interface Promise<T>
{
    Void VOID = null;

    /** same as {@link java.util.concurrent.CompletionStage#thenApply(Function)}  but usable in gwt */
    <U> Promise<U> thenApply(org.rapla.function.Function<? super T, ? extends U> fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)} but usable in gwt */
    Promise<Void> thenAccept(Consumer<? super T> fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenRun(Runnable)}  but usable in gwt */
    Promise<Void> thenRun(Command fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenCombine(CompletionStage, java.util.function.BiFunction)} but usable in gwt */
    <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenAcceptBoth(CompletionStage, java.util.function.BiConsumer)}   but usable in gwt */
    <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn);

    /** same as {@link java.util.concurrent.CompletionStage#runAfterBoth(CompletionStage, Runnable)}    but usable in gwt */
    Promise<Void> runAfterBoth(Promise<?> other, Command fn);

    /** same as {@link java.util.concurrent.CompletionStage#applyToEither(CompletionStage, Function)}     but usable in gwt */
    <U> Promise<U> applyToEither(Promise<? extends T> other, org.rapla.function.Function<? super T, U> fn);

    /** same as {@link java.util.concurrent.CompletionStage#acceptEither(CompletionStage, java.util.function.Consumer)}      but usable in gwt */
    Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn);

    /** same as {@link java.util.concurrent.CompletionStage#runAfterEither(CompletionStage, Runnable)}      but usable in gwt */
    Promise<Void> runAfterEither(Promise<?> other, Command fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenCompose(Function)}       but usable in gwt */
    <U> Promise<U> thenCompose(org.rapla.function.Function<? super T, ? extends Promise<U>> fn);

    /** same as {@link java.util.concurrent.CompletionStage#exceptionally(Function)}        but usable in gwt */
    Promise<T> exceptionally(org.rapla.function.Function<Throwable, ? extends T> fn);

    /** same as {@link java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer)}         but usable in gwt */
    Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn);

    /** same as {@link java.util.concurrent.CompletionStage#handle(java.util.function.BiFunction)}          but usable in gwt */
    <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    Observable<T> toObservable();
}

