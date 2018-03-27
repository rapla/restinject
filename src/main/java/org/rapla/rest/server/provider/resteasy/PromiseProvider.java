package org.rapla.rest.server.provider.resteasy;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.rapla.logger.NullLogger;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.sync.SynchronizedCompletablePromise;
import org.rapla.scheduler.sync.SynchronizedPromise;

import javax.ws.rs.ext.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Provider
public class PromiseProvider implements AsyncResponseProvider<Promise> {
    Function<Throwable, Throwable> exceptionMapper = ( throwable ) ->
    {
        // Resteasy 3.5.0 didnt wrap the promise exception  in an application exception so we do that here in case resteasy forgets abotu it
        if  (throwable instanceof ApplicationException)
        {
            return throwable;
        }
        return new ApplicationException( throwable);
    };
    @Override
    public CompletionStage toCompletionStage(Promise promise) {
        final CompletableFuture completableFuture = SynchronizedCompletablePromise.getCompletableFuture(promise, new NullLogger(), exceptionMapper);
        return completableFuture;
    }
}
