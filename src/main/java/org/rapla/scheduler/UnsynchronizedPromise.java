package org.rapla.scheduler;

import org.rapla.function.BiConsumer;
import org.rapla.function.BiFunction;
import org.rapla.function.Command;
import org.rapla.function.Consumer;
import org.rapla.function.Function;

import java.util.ArrayList;
import java.util.List;

public class UnsynchronizedPromise<T> implements Promise<T>
{
    private enum State
    {
        pending,
        fulfilled,
        rejected
    }

    protected T result;
    private Promise other;
    protected Throwable exception;
    private List<UnsynchronizedPromise> listeners;
    private State state = State.pending;
    private BiFunction< T,Object, T> fn;
    private Function<Throwable, ? extends T> exFn;

    public UnsynchronizedPromise()
    {
    }

    protected UnsynchronizedPromise(T result)
    {
        this.result = result;
        state = State.fulfilled;
    }

    protected UnsynchronizedPromise(Throwable ex)
    {
        this.exception = ex;
        state = State.rejected;
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Consumer fn)
    {
        this(parent, (act) -> {
            fn.accept(act);
            return null;
        });
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Command fn)
    {
        this(parent, (act) -> {
            fn.execute();
            return null;
        });
    }

    private UnsynchronizedPromise(Function<Throwable, ? extends T> exFn, UnsynchronizedPromise parent)
    {
        this.exFn = exFn;
        parent.initState(this, null);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Function<Object, T> fn)
    {
        this(parent, (a1, a2) -> fn.apply(a1), null);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Promise other, Command fn)
    {
        this(parent, (a, b) -> {
            fn.execute();
            return null;
        }, other);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Promise other, BiConsumer<Object, Object> fn)
    {
        this(parent, (a, b) -> {
            fn.accept(a, b);
            return null;
        }, other);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, BiFunction<T, Object, T> fn, Promise other)
    {
        this.fn = fn;
        this.other = other;
        parent.initState(this, other);
    }

    private void completed(final T result, Throwable ex)
    {
        if (other != null && ex == null)
        {
            final Promise<Object> promise = other.thenAccept((result2) -> changeState(result, result2, null));
            promise.exceptionally((ex2) -> {
                changeState(null, null, ex2);
                return null;
            });
        }
        else
        {
            changeState(result, null, ex);
        }
    }

    private void fireComplete(T result, Throwable ex)
    {
        if (listeners != null)
        {
            for (UnsynchronizedPromise listener : listeners)
            {
                listener.completed(result, ex);
            }
        }
    }

    private void initState(UnsynchronizedPromise nextPromise, Promise other)
    {
        if (state != State.pending)
        {
            if (other != null && exception == null)
            {
                final Promise<Object> promise = other.thenAccept((result2) -> nextPromise.changeState(result, result2, null));
                promise.exceptionally((ex2) -> {
                    nextPromise.changeState(null, null, ex2);
                    return null;
                });
            }
            else
            {
                nextPromise.changeState(result, null, exception);
            }
        }
        else
        {
            if (listeners == null)
            {
                listeners = new ArrayList<>();
            }
            listeners.add(nextPromise);
        }
    }

    @Override public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
    {
        return new UnsynchronizedPromise(this, fn);
    }

    @Override public Promise<Void> thenAccept(Consumer<? super T> action)
    {
        return new UnsynchronizedPromise(this, action);
    }

    @Override public Promise<Void> thenRun(Command action)
    {
        return new UnsynchronizedPromise(this, action);
    }

    @Override public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
    {
        return new UnsynchronizedPromise(this, fn, other);
    }

    @Override public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn)
    {
        return new UnsynchronizedPromise(this, other, fn);
    }

    @Override public Promise<Void> runAfterBoth(Promise<?> other, Command fn)
    {
        return new UnsynchronizedPromise(this, other, fn);
    }

    private static class BooleanContainer
    {
        boolean status = true;

        public synchronized boolean getAndSet(boolean b)
        {
            boolean old = status;
            status = b;
            return old;
        }
    }

    @Override public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
    {
        final UnsynchronizedCompletablePromise<U> resultPromise = new UnsynchronizedCompletablePromise<>();
        final BooleanContainer resultIsPending = new BooleanContainer();
        other.thenAccept((r) -> {
            final U apply = fn.apply(r);
            final boolean pending = resultIsPending.getAndSet(false);
            if (pending)
            {
                resultPromise.complete(apply);
            }
        }).exceptionally((ex) -> {
            final boolean pending = resultIsPending.getAndSet(false);
            if (pending)
            {
                resultPromise.completeExceptionally(ex);
            }
            return null;
        });
        this.thenAccept((r) -> {
            final U apply = fn.apply(r);
            final boolean pending = resultIsPending.getAndSet(false);
            if (pending)
            {
                resultPromise.complete(apply);
            }
        }).exceptionally((ex) -> {
            final boolean pending = resultIsPending.getAndSet(false);
            if (pending)
            {
                resultPromise.completeExceptionally(ex);
            }
            return null;
        });
        return resultPromise;
    }

    @Override public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn)
    {
        return applyToEither(other, (a) -> {
            fn.accept(a);
            return null;
        });
    }

    @Override public Promise<Void> runAfterEither(Promise<?> other, Command fn)
    {
        return acceptEither((Promise) other, (a) -> fn.execute());
    }

    @Override public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn)
    {
        final Promise<T> applyPromise = thenApply((a) -> {
            fn.accept(a, null);
            return a;
        });
        final Promise<T> exceptionallyPromise = applyPromise.exceptionally((ex) -> {
            fn.accept(null, ex);
            return null;
        });
        return exceptionallyPromise;
    }

    @Override public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
    {
        final Promise<U> applyPromise = thenApply((a) -> fn.apply(a, null));
        final Promise<U> exceptionallyPromise = applyPromise.exceptionally((ex) -> fn.apply(null, ex));
        return exceptionallyPromise;
    }

    @Override public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
    {
        final UnsynchronizedCompletablePromise<U> resultPromise = new UnsynchronizedCompletablePromise<>();
        this.thenAccept((r) -> {
            final Promise<U> apply = fn.apply(r);
            apply.thenAccept((r2) -> {
                resultPromise.complete(r2);
            }).exceptionally((ex) -> {
                resultPromise.completeExceptionally(ex);
                return null;
            });
        }).exceptionally((ex) -> {
            resultPromise.completeExceptionally(ex);
            return null;
        });
        return resultPromise;
    }

    @Override public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
    {
        return new UnsynchronizedPromise<T>(fn, this);
    }

    protected void changeState(T result, Object result2, Throwable ex)
    {
        if (state != State.pending)
        {
            throw new RuntimeException("Promise already " + state.name());
        }
        if (ex != null)
        {
            this.exception = ex;
        }
        else
        {
            if (fn != null)
            {
                try
                {
                    this.result = fn.apply(result, result2);
                }
                catch (Throwable ex2)
                {
                    exception = ex2;
                }
            }
            else
            {
                this.result = result;
            }
        }
        if (exception != null)
        {
            if (exFn != null)
            {
                try
                {
                    this.result = exFn.apply(ex);
                    exception = null;
                }
                catch (Throwable ex2)
                {
                    exception = ex2;
                }
            }
        }
        state = exception == null ? State.fulfilled : State.rejected;
        fireComplete(this.result, exception);
    }


}
