package org.rapla.server;

import java.io.IOException;

import javax.inject.Inject;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

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
