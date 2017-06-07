package org.rapla.scheduler;

import org.rapla.function.Command;
import org.rapla.function.Consumer;
import org.rapla.function.Function;

public interface Observable<T>
{
    <U> Observable<U> onNextApply(Function<? super T, ? extends U>  function);
    Observable<Void> onNextAccept(Consumer<T> consumer);
    Observable<Void> onNextRun(Command command);
    Observable<Void> onComplete(Command command);
    Observable<Void> exceptionally(Consumer<Throwable> errorFunction);
}
