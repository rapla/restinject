package org.rapla.scheduler.sync;

import org.rapla.scheduler.CompletablePromise;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class SynchronizedCompletablePromise<T> extends  SynchronizedPromise<T> implements CompletablePromise<T>
{
    CompletableFuture<T> f;
    SynchronizedCompletablePromise(Executor executor, CompletableFuture<T> f)
    {
        super(executor, f);
        this.f= f;
    }

    @Override
    public void complete(T value)
    {
        f.complete( value );
    }

    @Override
    public void completeExceptionally(Throwable ex)
    {
        f.completeExceptionally( ex);
    }
}
