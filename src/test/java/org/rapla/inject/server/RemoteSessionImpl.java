package org.rapla.inject.server;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.TestSingleton;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@RequestScoped
@DefaultImplementation(of=RemoteSession.class,context = InjectionContext.server)
public class RemoteSessionImpl implements RemoteSession
{
    String text;
    @Inject
    public RemoteSessionImpl(HttpServletRequest request, TestSingleton server)
    {
        text = server.getTest() + request.getRequestURL();
    }

    @Override public String toString()
    {
        return text;
    }
}
