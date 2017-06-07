package org.rapla.rest.client;

import org.rapla.function.BiConsumer;
import org.rapla.function.Consumer;
import org.rapla.function.Function;
import org.rapla.logger.Logger;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;

import java.util.concurrent.Executor;

public interface CustomConnector extends ExceptionDeserializer
{
    String getFullQualifiedUrl(String relativePath);
    String reauth(Class proxy) throws Exception;
    String getAccessToken();
    Logger getLogger();
    <T> CompletablePromise<T> createCompletable();
    <T> Promise<T> call(CommandScheduler.Callable<T> callable);
}
