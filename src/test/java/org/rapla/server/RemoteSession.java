package org.rapla.server;

import org.rapla.inject.server.RequestScoped;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@RequestScoped
public class RemoteSession
{
    String text;
    @Inject
    public RemoteSession(HttpServletRequest request, TestServer server)
    {
        text = server.getTest() + request.getRequestURL();
    }

    @Override public String toString()
    {
        return text;
    }
}
