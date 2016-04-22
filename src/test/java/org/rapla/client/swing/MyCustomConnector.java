package org.rapla.client.swing;

import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.gwt.MockProxy;
import org.rapla.rest.client.RaplaConnectException;

import java.io.IOException;
import java.lang.reflect.Constructor;


class MyCustomConnector implements CustomConnector
{

    String accessToken;

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
