package org.rapla.client.gwt;

import com.google.gwt.core.client.GWT;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.logger.Logger;
import org.rapla.logger.internal.JavaUtilLoggerForGwt;
import org.rapla.rest.SerializableExceptionInformation;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.CompletablePromise;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.client.gwt.GwtCommandScheduler;

import javax.inject.Inject;
import javax.inject.Singleton;

@DefaultImplementation(of=CustomConnector.class,context = InjectionContext.gwt)
@Singleton
public class GwtCustomConnector implements CustomConnector
{

    String accessToken;
    Logger logger = new JavaUtilLoggerForGwt("rapla");

    CommandScheduler scheduler = new GwtCommandScheduler(logger);
    @Inject
    public GwtCustomConnector()
    {

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
            if ( aClass.equals( NullPointerException.class.getCanonicalName()))
            {
                return new NullPointerException(exceptionInformations.getMessage());
            }
        }
        return new Exception(aClass + " " + exceptionInformations.getMessage() );
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
    public <T> Promise<T> call(CommandScheduler.Callable<T> supplier)
    {
        return scheduler.supply( supplier);
    }

    @Override public String getFullQualifiedUrl(String relativePath)
    {
        return GWT.getModuleBaseURL() + "rapla/" + relativePath;
    }
}
