package org.rapla.scheduler;

import org.rapla.scheduler.impl.UnsynchronizedPromise;

public class ResolvedPromise<T> extends UnsynchronizedPromise<T>
{
    final public static Promise<Void> VOID_PROMISE = new ResolvedPromise<Void>((Void) null);

    public ResolvedPromise(T result)
    {
        super(result);
    }

    public ResolvedPromise(Throwable ex)
    {
        super(ex);
    }
}
