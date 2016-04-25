package org.rapla.client.gwt;

import com.google.gwt.core.client.GWT;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.gwt.MockProxy;
import org.rapla.rest.client.RaplaConnectException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@DefaultImplementation(of=CustomConnector.class,context = InjectionContext.gwt)
@Singleton
public class GwtCustomConnector implements CustomConnector
{

    String accessToken;
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

    @Override public Exception deserializeException(SerializableExceptionInformation exceptionInformations)
    {
        String aClass = exceptionInformations.getExceptionClass();
        if ( aClass != null)
        {
        }
        return new Exception(aClass + " " + exceptionInformations.getMessage() + " " + exceptionInformations.getMessages());
        // throw new Au
    }

    @Override public Class[] getNonPrimitiveClasses()
    {
        return new Class[0];
    }

    @Override public String getFullQualifiedUrl(String relativePath)
    {
        return GWT.getModuleBaseURL() + "rapla/" + relativePath;
    }
}
