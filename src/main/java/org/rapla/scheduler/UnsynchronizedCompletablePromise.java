package org.rapla.scheduler;

public class UnsynchronizedCompletablePromise<T> extends UnsynchronizedPromise<T> implements CompletablePromise<T>
{
    public void complete(T result)
    {
        completed(result, null);
    }

    public void completeExceptionally(Throwable ex)
    {
        completed(null,ex);
    }
}
