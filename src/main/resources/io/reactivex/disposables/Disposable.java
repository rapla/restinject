package io.reactivex.disposables;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/**
 * Represents a disposable resource.
 */
@JsFunction
public interface Disposable {
    /**
     * Dispose the resource, the operation should be idempotent.
     */
    void dispose();

    /**
     * Returns true if this resource has been disposed.
     * @return true if this resource has been disposed
     */
    boolean isDisposed();
}
