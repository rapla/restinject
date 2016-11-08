package org.rapla.client.standalone;

import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.rapla.rest.client.swing.AbstractLocalJsonConnector;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class StandaloneConnector extends AbstractLocalJsonConnector
{
    private final LocalConnector connector;
    private final Semaphore waitForSemarphore = new Semaphore(0);
    private final Semaphore waitForStart = new Semaphore(0);

    public StandaloneConnector(LocalConnector connector)
    {
        this.connector = connector;

        connector.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener()
        {
            @Override public void lifeCycleStarted(LifeCycle event)
            {
                waitForStart.release();
            }
        });
    }

    // Called with reflection. Dont delete
    public void requestFinished()
    {
        waitForSemarphore.release();
    }

    @Override protected String doSend(String rawHttpRequest)
    {
        if (!connector.isRunning() && !connector.isStopped() && !connector.isStopped())
        {
            try
            {
                waitForStart.tryAcquire(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException("Server did not answer within 60 sec: " + e.getMessage(), e);
            }
        }
        LocalConnector.LocalEndPoint endpoint = connector.executeRequest(rawHttpRequest);
        try
        {
            // Wait max 60 seconds
            waitForSemarphore.tryAcquire(10, TimeUnit.SECONDS);
            //endpoint.waitUntilClosed();
            Thread.sleep(500);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("Server did not answer within 60 sec: " + e.getMessage(), e);
        }

        final ByteBuffer byteBuffer = endpoint.takeOutput();
        final String s = byteBuffer == null ? null : BufferUtil.toString(byteBuffer, StandardCharsets.UTF_8);
        return s;
    }

}
