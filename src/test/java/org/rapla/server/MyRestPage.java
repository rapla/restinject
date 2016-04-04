package org.rapla.server;

import org.rapla.common.MyRestApi;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import javax.inject.Inject;
import java.io.IOException;

@DefaultImplementation(context=InjectionContext.server, of=MyRestApi.class)
public class MyRestPage implements MyRestApi
{
    @Inject
    public MyRestPage()
    {
    }

    @Override
    public String test(String username, String password)
    {
        if (password != null && password.equals("secret"))
        {
            return "Hello " + username;
        }
        else
        {
            return "login failed";
        }
    }

    @Override
    public String test_(String username, String password) throws IOException
    {
        if (password != null && password.equals("secret"))
        {
            return "Hello " + username;
        }
        else
        {
            throw new IOException("login failed");
        }
    }
}
