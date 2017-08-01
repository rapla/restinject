package org.rapla.scheduler;

import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.util.concurrent.CompletionStage;

/** same as {@link java.util.concurrent.CompletionStage} but usable in gwt */
public interface Promise<T>
{
    Void VOID = null;

    /** same as {@link java.util.concurrent.CompletionStage#thenApply(java.util.function.Function)}  but usable in gwt */
    <U> Promise<U> thenApply(Function<? super T, ? extends U> fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenAccept(java.util.function.Consumer)} but usable in gwt */
    Promise<Void> thenAccept(Consumer<? super T> fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenRun(Runnable)}  but usable in gwt */
    Promise<Void> thenRun(Action fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenCombine(CompletionStage, java.util.function.BiFunction)} but usable in gwt */
    <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenAcceptBoth(CompletionStage, java.util.function.BiConsumer)}   but usable in gwt */
    <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn);

    /** same as {@link java.util.concurrent.CompletionStage#runAfterBoth(CompletionStage, Runnable)}    but usable in gwt */
    Promise<Void> runAfterBoth(Promise<?> other, Action fn);

    /** same as {@link java.util.concurrent.CompletionStage#applyToEither(CompletionStage, java.util.function.Function)}     but usable in gwt */
    <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn);

    /** same as {@link java.util.concurrent.CompletionStage#acceptEither(CompletionStage, java.util.function.Consumer)}      but usable in gwt */
    Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn);

    /** same as {@link java.util.concurrent.CompletionStage#runAfterEither(CompletionStage, Runnable)}      but usable in gwt */
    Promise<Void> runAfterEither(Promise<?> other, Action fn);

    /** same as {@link java.util.concurrent.CompletionStage#thenCompose(java.util.function.Function)}       but usable in gwt */
    <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn);

    /** same as {@link java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)}        but usable in gwt */
    Promise<T> exceptionally(Function<Throwable, ? extends T> fn);

    /** same as {@link java.util.concurrent.CompletionStage#whenComplete(java.util.function.BiConsumer)}         but usable in gwt */
    Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn);

    /** same as {@link java.util.concurrent.CompletionStage#handle(java.util.function.BiFunction)}          but usable in gwt */
    <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);
}

