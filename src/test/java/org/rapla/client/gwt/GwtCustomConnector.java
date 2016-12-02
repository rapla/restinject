package org.rapla.client.gwt;

import com.google.gwt.core.client.GWT;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.logger.Logger;
import org.rapla.logger.internal.JavaUtilLoggerForGwt;
import org.rapla.rest.SerializableExceptionInformation;
import org.rapla.rest.client.CustomConnector;

import javax.inject.Inject;
import javax.inject.Singleton;

@DefaultImplementation(of=CustomConnector.class,context = InjectionContext.gwt)
@Singleton
public class GwtCustomConnector implements CustomConnector
{

    String accessToken;
    Logger logger = new JavaUtilLoggerForGwt("rapla");

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
        return new Exception(aClass + " " + exceptionInformations.getMessage() + " " + exceptionInformations.getMessages());
        // throw new Au
    }

    public Logger getLogger()
    {
        return logger;
    }

    @Override public String getFullQualifiedUrl(String relativePath)
    {
        return GWT.getModuleBaseURL() + "rapla/" + relativePath;
    }
}
