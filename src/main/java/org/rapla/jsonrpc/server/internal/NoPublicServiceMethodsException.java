package org.rapla.jsonrpc.server.internal;

import org.rapla.jsonrpc.common.RemoteJsonMethod;

/**
 * Created by Christopher on 02.09.2015.
 */
public class NoPublicServiceMethodsException extends Exception
{
    public NoPublicServiceMethodsException(Class class1)
    {
        super("No public service methods declared in " + class1 + " Did you forget the " + RemoteJsonMethod.class+ " proxy?");
    }
}
