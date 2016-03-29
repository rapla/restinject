package org.rapla.server;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.rapla.common.TestSingleton;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(of = RemoteSession.class, context = InjectionContext.server)
public class RemoteSessionImpl implements RemoteSession
{
    String text;

    @Inject
    public RemoteSessionImpl(TestSingleton server)
    {
        text = server.getTest();
    }

    @Override
    public String toString(HttpServletRequest request)
    {
        return text + request.getRequestURL();
    }
}
