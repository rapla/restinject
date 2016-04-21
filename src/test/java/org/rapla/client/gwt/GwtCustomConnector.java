package org.rapla.client.gwt;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.gwt.MockProxy;
import org.rapla.rest.client.swing.RaplaConnectException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Constructor;

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

    @Override public Exception getConnectError(IOException ex)
    {
        return new RaplaConnectException("Connection Error " + ex.getMessage());
    }

    @Override public MockProxy getMockProxy()
    {
        return null;
    }
}
