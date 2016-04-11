/*
package org.rapla.scheduler.impl;

import org.rapla.scheduler.Promise;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

class CompletionStageAdapter<T> implements CompletionStage<T>
{
    private final Promise<T> promise;

    public CompletionStageAdapter(Promise<T> promise)
    {
        this.promise = promise;
    }

    @Override public <U> CompletionStage<U> thenApply(java.util.function.Function<? super T, ? extends U> fn)
    {
        Promise.Function<? super T, ? extends U> fun = (s) -> fn.apply(s);
        return v(promise.thenApply(fun));
    }

    protected <T> CompletionStage<T> v(final Promise<T> promise)
    {
        return new CompletionStageAdapter<>(promise);
    }


    @Override public <U> CompletionStage<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn)
    {
        return thenApply(fn);
    }

    @Override public <U> CompletionStage<U> thenApplyAsync(java.util.function.Function<? super T, ? extends U> fn, Executor executor)
    {
        return thenApply(fn);
    }

    @Override public CompletionStage<Void> thenAccept(java.util.function.Consumer<? super T> fn)
    {
        Promise.Consumer<? super T> fun = (s) -> fn.accept(s);
        return v(promise.thenAccept(fun));
    }

    @Override public CompletionStage<Void> thenAcceptAsync(java.util.function.Consumer<? super T> fn)
    {
        return thenAccept(fn);
    }

    @Override public CompletionStage<Void> thenAcceptAsync(java.util.function.Consumer<? super T> fn, Executor executor)
    {
        return thenAccept(fn);
    }

    @Override public CompletionStage<Void> thenRun(Runnable fn)
    {
        return v(promise.thenRun(fn));
    }

    @Override public CompletionStage<Void> thenRunAsync(Runnable fn)
    {
        return thenRun(fn);
    }

    @Override public CompletionStage<Void> thenRunAsync(Runnable fn, Executor executor)
    {
        return thenRun(fn);
    }

    @Override public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            java.util.function.BiFunction<? super T, ? super U, ? extends V> fn)
    {
        Promise.BiFunction<? super T, ? super U, ? extends V> fun = (s, t) -> fn.apply(s, t);
        Promise<? extends U> otherPromise = w(other);
        return v(promise.thenCombine(otherPromise, fun));
    }

    @Override public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            java.util.function.BiFunction<? super T, ? super U, ? extends V> fn)
    {
        return thenCombine(other, fn);
    }

    @Override public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            java.util.function.BiFunction<? super T, ? super U, ? extends V> fn, Executor executor)
    {
        return thenCombine(other, fn);
    }

    protected <U> Promise<U> w(CompletionStage<U> stage)
    {
    }

    @Override public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other, java.util.function.BiConsumer<? super T, ? super U> fn)
    {
        Promise otherP = w(other);
        Promise.BiConsumer<? super T, ? super U> fun = (s, t) -> fn.accept(s, t);
        return v(promise.thenAcceptBoth(otherP, fun));
    }

    @Override public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            java.util.function.BiConsumer<? super T, ? super U> action)
    {
        return thenAcceptBoth(other, action);
    }

    @Override public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            java.util.function.BiConsumer<? super T, ? super U> action, Executor executor)
    {
        return thenAcceptBoth(other, action);
    }

    @Override public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action)
    {
        Promise otherP = w(other);
        return v(promise.runAfterBoth(otherP, action));
    }

    @Override public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action)
    {
        return runAfterBoth(other, action);
    }

    @Override public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor)
    {
        return runAfterBoth(other, action);
    }

    @Override public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn)
    {
        Promise otherP = w(other);
        Promise.Function<? super T, U> fun = (t) -> fn.apply(t);
        return v(promise.applyToEither(otherP, fun));
    }

    @Override public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn)
    {
        return applyToEither(other, fn);
    }

    @Override public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, java.util.function.Function<? super T, U> fn,
            Executor executor)
    {
        return applyToEither(other, fn);
    }

    @Override public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, java.util.function.Consumer<? super T> fn)
    {
        Promise otherP = w(other);
        Promise.Consumer<? super T> fun = (t) -> fn.accept(t);
        return v(promise.acceptEither(otherP, fun));
    }

    @Override public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action)
    {
        return acceptEither(other, action);
    }

    @Override public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, java.util.function.Consumer<? super T> action,
            Executor executor)
    {
        return acceptEither(other, action);
    }

    @Override public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action)
    {
        Promise otherP = w(other);
        return v(promise.runAfterEither(otherP, action));
    }

    @Override public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action)
    {
        return runAfterEither(other, action);
    }

    @Override public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor)
    {
        return runAfterEither(other, action);
    }

    @Override public <U> CompletionStage<U> thenCompose(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn)
    {

        Promise.Function<? super T, ? extends Promise<U>> fun = (s) -> w(fn.apply(s));
        return v(promise.thenCompose(fun));
    }

    @Override public <U> CompletionStage<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn)
    {
        return thenCompose(fn);
    }

    @Override public <U> CompletionStage<U> thenComposeAsync(java.util.function.Function<? super T, ? extends CompletionStage<U>> fn, Executor executor)
    {
        return thenCompose(fn);
    }

    @Override public CompletionStage<T> exceptionally(java.util.function.Function<Throwable, ? extends T> fn)
    {
        Promise.Function<Throwable, ? extends T> fun = (s) -> fn.apply(s);
        return v(promise.exceptionally(fun));
    }

    @Override public CompletionStage<T> whenComplete(java.util.function.BiConsumer<? super T, ? super Throwable> fn)
    {
        Promise.BiConsumer<? super T, ? super Throwable> fun = (s, t) -> fn.accept(s, t);
        return v(promise.whenComplete(fun));
    }

    @Override public CompletionStage<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> fn)
    {
        return whenComplete(fn);
    }

    @Override public CompletionStage<T> whenCompleteAsync(java.util.function.BiConsumer<? super T, ? super Throwable> fn, Executor executor)
    {
        return whenComplete(fn);
    }

    @Override public <U> CompletionStage<U> handle(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn)
    {
        Promise.BiFunction<? super T, Throwable, ? extends U> fun = (s, t) -> fn.apply(s, t);
        return v(promise.handle(fun));
    }

    @Override public <U> CompletionStage<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn)
    {
        return handle(fn);
    }

    @Override public <U> CompletionStage<U> handleAsync(java.util.function.BiFunction<? super T, Throwable, ? extends U> fn, Executor executor)
    {
        return handle(fn);
    }

    @Override public CompletableFuture<T> toCompletableFuture()
    {
        throw new UnsupportedOperationException();
    }
}
*/