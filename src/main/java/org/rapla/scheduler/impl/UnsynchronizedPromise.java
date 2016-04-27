package org.rapla.scheduler.impl;

import org.rapla.scheduler.Promise;

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

    private Object result;
    private Promise other;
    private Throwable exception;
    private List<UnsynchronizedPromise> listeners;
    private State state = State.pending;
    private BiFunction<Object, Object, Object> fn;
    private Function<Throwable, ? extends Object> exFn;

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

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Runnable fn)
    {
        this(parent, (act) -> {
            fn.run();
            return null;
        });
    }

    private UnsynchronizedPromise(Function<Throwable, ? extends Object> exFn, UnsynchronizedPromise parent)
    {
        this.exFn = exFn;
        parent.initState(this, null);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Function<Object, Object> fn)
    {
        this(parent, (a1, a2) -> fn.apply(a1), null);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Promise other, Runnable fn)
    {
        this(parent, (a, b) -> {
            fn.run();
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

    private UnsynchronizedPromise(UnsynchronizedPromise parent, BiFunction<Object, Object, Object> fn, Promise other)
    {
        this.fn = fn;
        this.other = other;
        parent.initState(this, other);
    }

    private void completed(final Object result, Throwable ex)
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

    private void fireComplete(Object result, Throwable ex)
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

    @Override public Promise<Void> thenRun(Runnable action)
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

    @Override public Promise<Void> runAfterBoth(Promise<?> other, Runnable fn)
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
        final UnsynchronizedPromise<U> resultPromise = new UnsynchronizedPromise<>();
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
                resultPromise.abort(ex);
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
                resultPromise.abort(ex);
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

    @Override public Promise<Void> runAfterEither(Promise<?> other, Runnable fn)
    {
        return acceptEither((Promise) other, (a) -> fn.run());
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
        final UnsynchronizedPromise<U> resultPromise = new UnsynchronizedPromise<>();
        this.thenAccept((r) -> {
            final Promise<U> apply = fn.apply(r);
            apply.thenAccept((r2) -> {
                resultPromise.complete(r2);
            }).exceptionally((ex) -> {
                resultPromise.abort(ex);
                return null;
            });
        }).exceptionally((ex) -> {
            resultPromise.abort(ex);
            return null;
        });
        return resultPromise;
    }

    @Override public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
    {
        return new UnsynchronizedPromise<T>(fn, this);
    }

    private void changeState(Object result, Object result2, Throwable ex)
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

    public void complete(T result)
    {
        changeState(result, null, null);
    }

    public void abort(Throwable ex)
    {
        changeState(null, null, ex);
    }

}
