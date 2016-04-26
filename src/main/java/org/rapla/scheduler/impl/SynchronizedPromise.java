package org.rapla.scheduler.impl;

import org.rapla.scheduler.Promise;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

class SynchronizedPromise<T> implements Promise<T>
{

    final Executor promiseExecuter;
    final CompletionStage f;

    SynchronizedPromise(Executor executor, CompletionStage f)
    {
        this.promiseExecuter = executor;
        this.f = f;
    }

    protected <U> Promise<U> w(CompletionStage<U> stage)
    {
        return new SynchronizedPromise<U>(promiseExecuter, stage);
    }

    protected <T> CompletionStage<T> v(final Promise<T> promise)
    {
        if (promise instanceof SynchronizedPromise)
        {
            return ((SynchronizedPromise) promise).f;
        }
        else
        {
            CompletableFuture<T> future = new CompletableFuture<T>();
            promise.thenAccept((t) -> future.complete(t)).exceptionally((ex) -> {
                future.completeExceptionally(ex);
                return null;
            });
            return future;
        }
    }

    @Override public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
    {
        final java.util.function.Function<? super T, ? extends U> fun = (t) -> {
            try
            {
                return fn.apply(t);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.thenApplyAsync(fun, promiseExecuter));
    }

    @Override public Promise<Void> thenAccept(Consumer<? super T> fn)
    {
        final java.util.function.Consumer<T> consumer = (a) -> {
            try
            {
                fn.accept(a);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.thenAcceptAsync(consumer, promiseExecuter));
    }

    @Override public Promise<Void> thenRun(Runnable action)
    {
        return w(f.thenRunAsync(action, promiseExecuter));
    }

    @Override public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
    {
        final java.util.function.BiFunction<? super T, ? super U, ? extends V> bifn = (t, u) -> {
            try
            {
                return fn.apply(t, u);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        final CompletionStage<? extends U> v = v(other);
        return w(f.thenCombineAsync(v, bifn, promiseExecuter));
    }

    @Override public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn)
    {
        final java.util.function.BiConsumer<? super T, ? super U> biConsumer = (t, u) -> {
            try
            {
                fn.accept(t, u);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        final CompletionStage<? extends U> v = v(other);
        return w(f.thenAcceptBothAsync(v, biConsumer, promiseExecuter));
    }

    @Override public Promise<Void> runAfterBoth(Promise<?> other, Runnable action)
    {
        return w(f.runAfterBothAsync(v(other), action, promiseExecuter));
    }

    @Override public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
    {
        final java.util.function.Function<? super T, ? extends U> fun = (t) -> {
            try
            {
                return fn.apply(t);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.applyToEitherAsync(v(other), fun, promiseExecuter));
    }

    @Override public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn)
    {
        final java.util.function.Consumer<? super T> action = (t) -> {
            try
            {
                fn.accept(t);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.acceptEitherAsync(v(other), action, promiseExecuter));
    }

    @Override public Promise<Void> runAfterEither(Promise<?> other, Runnable fn)
    {
        return w(f.runAfterEitherAsync(v(other), fn, promiseExecuter));
    }

    @Override public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
    {
        final java.util.function.Function<? super T, ? extends CompletionStage<U>> fun = (t) -> {
            try
            {
                return v(fn.apply(t));
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.thenComposeAsync(fun, promiseExecuter));
    }

    @Override public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
    {
        final java.util.function.Function<Throwable, ? extends T> fun = (t) -> {
            try
            {
                if (t instanceof PromiseRuntimeException)
                {
                    return fn.apply(t.getCause());
                }
                return fn.apply(t);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.exceptionally(fun));
    }

    @Override public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn)
    {
        final java.util.function.BiConsumer<? super T, ? super Throwable> bifn = (t, u) -> {
            try
            {
                fn.accept(t, u);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.whenCompleteAsync(bifn, promiseExecuter));
    }

    @Override public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
    {
        final java.util.function.BiFunction<? super T, Throwable, ? extends U> bifn = (t, u) -> {
            try
            {
                return fn.apply(t, u);
            }
            catch (Exception e)
            {
                throw new PromiseRuntimeException(e);
            }
        };
        return w(f.handleAsync(bifn, promiseExecuter));
    }

    static class PromiseRuntimeException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public PromiseRuntimeException(Throwable cause)
        {
            super(cause);
        }

        @Override public synchronized Throwable getCause()
        {
            return super.getCause();
        }

    }
}
