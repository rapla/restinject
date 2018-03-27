package org.rapla.rest.server.provider.resteasy;

import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.rapla.logger.NullLogger;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.sync.SynchronizedCompletablePromise;
import org.rapla.scheduler.sync.SynchronizedPromise;

import javax.ws.rs.ext.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Provider
public class PromiseProvider implements AsyncResponseProvider<Promise> {
    @Override
    public CompletionStage toCompletionStage(Promise promise) {
        if ( promise instanceof SynchronizedPromise)
        {
            return ((SynchronizedPromise)promise).getCompletionStage();
        }
        else
        {
            final CompletableFuture completableFuture = SynchronizedCompletablePromise.getCompletableFuture(promise, new NullLogger());
            return completableFuture;
        }
    }
}
