package org.rapla.scheduler;

public class UnsynchronizedCompletablePromise<T> extends UnsynchronizedPromise<T> implements CompletablePromise<T>
{
    public void complete(T result)
    {
        changeState(result, null, null);
    }

    public void completeExceptionally(Throwable ex)
    {
        changeState(null, null, ex);
    }
}
