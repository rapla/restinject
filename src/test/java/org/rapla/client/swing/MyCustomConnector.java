package org.rapla.client.swing;

import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.SerializableExceptionInformation;

import java.lang.reflect.Constructor;


public class MyCustomConnector implements CustomConnector
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



    @Override public String getFullQualifiedUrl(String relativePath)
    {
        return "http://localhost:8052/rapla/" + relativePath;
    }
}
