package org.rapla.scheduler;

import org.rapla.function.BiFunction;
import org.rapla.function.Command;
import org.rapla.function.Consumer;
import org.rapla.function.Function;

import java.util.ArrayList;
import java.util.List;

public class SimpleSubject<T> implements Observable<T>
{
    private enum State
    {
        pending,
        fulfilled,
        rejected
    }

    State state;
    protected Throwable exception;
    private List<SimpleSubject> listeners;
    private BiFunction<T, Object, T> fn;
    private Command completeFunction;
    private Consumer<Throwable> exceptionFn;

    public SimpleSubject()
    {
    }

    @Override
    public <U> Observable<U> onNextApply(Function<? super T, ? extends U> function)
    {
        return new SimpleSubject(this, function);
    }

    @Override
    public Observable<Void> onNextAccept(Consumer<T> consumer)
    {
        return new SimpleSubject(this, consumer);
    }

    @Override
    public Observable<Void> onNextRun(Command command)
    {
        return new SimpleSubject(this, command);
    }

    @Override
    public Observable<Void> onComplete(Command completeCommand)
    {
        return new SimpleSubject(this, null, completeCommand, null);
    }

    private SimpleSubject(SimpleSubject parent, Function<Object, T> fn)
    {
        this(parent, (a1, a2) -> fn.apply(a1), null, null);
    }

    private SimpleSubject(SimpleSubject parent, Consumer fn)
    {
        this(parent, (act) ->
        {
            fn.accept(act);
            return null;
        });
    }

    private SimpleSubject(SimpleSubject parent, Command fn)
    {
        this(parent, (act) ->
        {
            fn.execute();
            return null;
        });
    }

    public SimpleSubject(SimpleSubject parent, BiFunction<T, Object, T> nextFunction, Command completeFunction, Consumer<Throwable> exceptionFn)
    {
        parent.initState(this);
        this.fn = nextFunction;
        this.completeFunction = completeFunction;
        this.exceptionFn = exceptionFn;
    }

    protected void completed(Throwable ex)
    {

        if (state != State.pending)
        {
            throw new IllegalStateException("Observable already completed: " + state.name());
        }
        if ( exception == null)
        {
            try
            {
                completeFunction.execute();
            }
            catch (Exception e)
            {
                exception = e;
            }
        }
        if (exception != null)
        {
            if (exceptionFn != null)
            {
                try
                {
                    exceptionFn.accept(ex);
                }
                catch (Throwable ex2)
                {
                    exception = ex2;
                }
            }
        }
        state = exception == null ? State.fulfilled : State.rejected;
        fireComplete(exception);
    }

    public void onNext(T result)
    {
        if (fn != null)
        {
            try
            {
                fn.apply(result, null);
            }
            catch (Exception e)
            {
                completed(e);
                return;
            }
        }
        if (state != State.pending)
        {
            throw new IllegalStateException("Observable already completed: " + state.name());
        }
        fireNext(result);
    }

    public void onComplete()
    {
        completed(null);
    }

    public void onError(Throwable exception)
    {
        completed(exception);
    }

    private void fireComplete(Throwable ex)
    {
        if (listeners != null)
        {
            for (SimpleSubject listener : listeners)
            {
                listener.completed(ex);
            }
        }
    }

    private void fireNext(T result)
    {
        if (listeners != null)
        {
            for (SimpleSubject listener : listeners)
            {
                listener.onNext(result);
            }
        }
    }

    private void initState(SimpleSubject next)
    {
        if (state != State.pending)
        {
            next.completed(exception);
        }
        else
        {
            if (listeners == null)
            {
                listeners = new ArrayList<>();
            }
            listeners.add(next);
        }
    }

    @Override
    public Observable<Void> exceptionally(Consumer<Throwable> exceptionFn)
    {
        return new SimpleSubject<Void>(this, null, null, exceptionFn);
    }

}
