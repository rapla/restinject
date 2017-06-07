package org.rapla.scheduler;

import org.rapla.function.Command;
import org.rapla.function.Consumer;
import org.rapla.function.Function;

public class ObservableFromPromise<T> implements Observable<T>
{
    Promise<T> promise;
    public ObservableFromPromise(Promise<T> promise)
    {
        this.promise = promise;
    }

    @Override
    public <U> Observable<U> onNextApply(Function<? super T, ? extends U>  function)
    {
        return new ObservableFromPromise<U>( promise.thenApply( function));
    }

    @Override
    public Observable<Void> onNextAccept(Consumer<T> consumer)
    {
        return new ObservableFromPromise<Void>(promise.thenAccept(consumer));
    }

    @Override
    public Observable<Void> onNextRun(Command command)
    {
        return new ObservableFromPromise<Void>(promise.thenRun(command));
    }

    @Override
    public Observable<Void> onComplete(Command command)
    {
        final Promise<Void> completed = promise.handle((value,ex) ->
        {
            if (ex == null)
            {
                command.execute();
            }
            return  null;
        });
        return new ObservableFromPromise<Void>(completed);

    }

    @Override
    public Observable<Void> exceptionally(Consumer<Throwable> errorFunction)
    {
        final Promise<Void> exceptionally = promise.handle((value,ex) ->
        {
            if (ex != null)
            {
                errorFunction.accept(ex);
            }
            return  null;
        });
        return new ObservableFromPromise<Void>(exceptionally);
    }
}
