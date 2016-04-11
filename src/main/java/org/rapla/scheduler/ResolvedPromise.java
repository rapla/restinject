package org.rapla.scheduler;

public class ResolvedPromise<T> implements Promise<T>
{
    final public static ResolvedPromise<Void> VOID_PROMISE = new ResolvedPromise<Void>((Void) null);
    final T t;
    final Throwable ex;
    public ResolvedPromise(T t)
    {
        this.t = t;
        this.ex = null;
    }

    public ResolvedPromise(Throwable throwable)
    {
        this.t = null;
        this.ex = throwable;
    }

    public T get() throws Throwable
    {
        if ( ex != null)
        {
            throw ex;
        }
        return t;
    }

    @Override public <U> Promise<U> thenApply(Function<? super T, ? extends U> fn)
    {
        if ( ex == null)
        {
            try
            {
                final U apply = fn.apply(t);
                return new ResolvedPromise<U>(apply);
            }
            catch (Exception ex )
            {
                return new ResolvedPromise<U>(ex);
            }
        }
        return new ResolvedPromise<U>(this.ex);
    }

    @Override public Promise<Void> thenAccept(Consumer<? super T> fn)
    {
        if ( ex == null)
        {
            try
            {
                fn.accept(t);
                return new ResolvedPromise<Void>((Void)null);
            }
            catch (Exception ex )
            {
                return new ResolvedPromise<Void>(ex);
            }
        }
        return new ResolvedPromise<Void>(this.ex);
    }

    @Override public Promise<Void> thenRun(Runnable action)
    {
        if ( ex == null)
        {
            try
            {
                action.run();
                return new ResolvedPromise<Void>((Void)null);
            }
            catch (Exception ex )
            {
                return new ResolvedPromise<Void>(ex);
            }
        }
        return new ResolvedPromise<Void>(this.ex);
    }

    @Override public <U, V> Promise<V> thenCombine(Promise<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn)
    {
        if ( ex== null)
        {
            return other.thenApply(( u) -> fn.apply(t,u));
        }
        return new ResolvedPromise<V>( ex);
    }

    @Override public <U> Promise<Void> thenAcceptBoth(Promise<? extends U> other, BiConsumer<? super T, ? super U> fn)
    {
        if ( ex== null)
        {
            return other.thenAccept(( u) -> fn.accept(t,u));
        }
        return new ResolvedPromise<Void>( ex);
    }

    @Override public Promise<Void> runAfterBoth(Promise<?> other, Runnable fn)
    {
        if ( ex== null)
        {
            return other.thenRun(fn);
        }
        return new ResolvedPromise<Void>( ex);
    }

    @Override public <U> Promise<U> applyToEither(Promise<? extends T> other, Function<? super T, U> fn)
    {
        return thenApply(fn);
    }

    @Override public Promise<Void> acceptEither(Promise<? extends T> other, Consumer<? super T> fn)
    {
        return thenAccept( fn);
    }

    @Override public Promise<Void> runAfterEither(Promise<?> other, Runnable fn)
    {
        return thenRun( fn);
    }

    @Override public <U> Promise<U> thenCompose(Function<? super T, ? extends Promise<U>> fn)
    {
        if ( ex == null)
        {
            try
            {
                final Promise<U> apply = fn.apply(t);
                return apply;
            }
            catch (Exception e)
            {
                return new ResolvedPromise<U>(e);
            }
        }
        else
        {
            return new ResolvedPromise<U>(ex);
        }
    }

    @Override public Promise<T> exceptionally(Function<Throwable, ? extends T> fn)
    {
        if ( ex == null)
        {
            return this;
        }
        else
        {
            try
            {
                final T apply = fn.apply(ex);
                return new ResolvedPromise<T>(apply);
            }
            catch (Exception e)
            {
                return new ResolvedPromise<T>( e);
            }
        }
    }

    @Override public Promise<T> whenComplete(BiConsumer<? super T, ? super Throwable> fn)
    {
        try
        {
            fn.accept(t,ex);
        }
        catch (Exception e)
        {
            return  new ResolvedPromise<T>(e);
        }
        return this;
    }

    @Override public <U> Promise<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
    {
        try
        {
            final U apply = fn.apply(t, ex);
            return new ResolvedPromise<U>(apply);
        }
        catch (Exception e)
        {
            return  new ResolvedPromise<U>(e);
        }
    }
}
