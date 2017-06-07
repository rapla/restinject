package org.rapla.client.swing;

import org.rapla.function.Command;
import org.rapla.logger.ConsoleLogger;
import org.rapla.logger.Logger;
import org.rapla.rest.client.AsyncCallback;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.SerializableExceptionInformation;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.client.swing.SwingScheduler;
import org.rapla.scheduler.sync.UtilConcurrentCommandScheduler;

import java.lang.reflect.Constructor;


public class MyCustomConnector implements CustomConnector
{

    String accessToken;
    Logger logger;
    CommandScheduler scheduler;
    public MyCustomConnector(Logger logger, CommandScheduler scheduler)
    {
        this.logger = logger;
        this.scheduler = scheduler;
    }

    @Override public String reauth(Class proxy) throws Exception
    {
        return accessToken;
    }

    @Override public String getAccessToken()
    {
        return accessToken;
    }

    @Override public Exception deserializeException(SerializableExceptionInformation exceptionInformations, int responseCode)
    {
        String aClass = exceptionInformations.getExceptionClass();
        if ( aClass != null)
        {
            try
            {
                final Class<?> aClass1 = Class.forName(aClass);
                Constructor<?> constructor = aClass1.getConstructor(String.class);
                if (constructor != null)
                {
                    return (Exception)constructor.newInstance(exceptionInformations.getMessage());
                }
                else
                {
                    constructor = aClass1.getConstructor();
                    if ( constructor != null)
                    {
                        return (Exception) constructor.newInstance();
                    }
                }
            }
            catch ( Exception ex)
            {

            }
        }
        return new Exception(aClass + " " + exceptionInformations.getMessage() + " " + exceptionInformations.getMessages());
        // throw new Au
    }


    public Logger getLogger()
    {
        return logger;
    }

    @Override
    public <T> CompletablePromise<T> createCompletable()
    {
        return scheduler.createCompletable();
    }

    @Override
    public  <T> Promise<T> call(CommandScheduler.Callable<T> callback)
    {
        return scheduler.supply( callback);
    }

    @Override public String getFullQualifiedUrl(String relativePath)
    {
        return "http://localhost:8052/rapla/" + relativePath;
    }
}
