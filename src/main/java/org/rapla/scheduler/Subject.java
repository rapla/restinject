package org.rapla.scheduler;

import io.reactivex.Observer;

public interface Subject<T> extends Observable<T>,Observer<T> {
}
