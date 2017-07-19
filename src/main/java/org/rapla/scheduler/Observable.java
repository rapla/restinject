package org.rapla.scheduler;

import org.rapla.function.Command;
import org.rapla.function.Consumer;
import org.rapla.function.Function;

public interface Observable<T>
{
    Observable<T> subscribe();
}
