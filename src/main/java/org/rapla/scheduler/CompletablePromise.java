package org.rapla.scheduler;

import jsinterop.annotations.JsType;

@JsType
public interface CompletablePromise<T> extends Promise<T>
{
    void complete(T value);
    void completeExceptionally(Throwable ex);
    boolean isDone();
}
