package org.rapla.scheduler;


import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import java.util.ArrayList;
import java.util.List;

public class UnsynchronizedPromise<T> implements CompletablePromise<T>
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
    private Function<Throwable, ? extends T> exceptionFn;

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

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Action fn)
    {
        this(parent, (act) -> {
            fn.run();
            return null;
        });
    }

    private UnsynchronizedPromise(Function<Throwable, ? extends T> exceptionFn, UnsynchronizedPromise parent)
    {
        this.exceptionFn = exceptionFn;
        parent.initState(this, null);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Function<Object, T> fn)
    {
        this(parent, (a1, a2) -> fn.apply(a1), null);
    }

    private UnsynchronizedPromise(UnsynchronizedPromise parent, Promise other, Action fn)
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

    private UnsynchronizedPromise(UnsynchronizedPromise parent, BiFunction<T, Object, T> fn, Promise other)
    {
        this.fn = fn;
        this.other = other;
        parent.initState(this, other);
    }

    protected void completed(final T result, Throwable ex)
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

    @Override public Promise<Void> thenRun(Action action)
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

    @Override public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
    {
        final UnsynchronizedPromise<U> resultPromise = new UnsynchronizedPromise<>();
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
            throw new IllegalStateException("Promise already completed: " + state.name());
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
            if (exceptionFn != null)
            {
                try
                {
                    this.result = exceptionFn.apply(ex);
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
        completed(result, null);
    }

    public void completeExceptionally(Throwable ex)
    {
        completed(null,ex);
    }

}
