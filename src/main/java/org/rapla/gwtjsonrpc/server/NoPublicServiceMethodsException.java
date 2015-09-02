package org.rapla.gwtjsonrpc.server;

/**
 * Created by Christopher on 02.09.2015.
 */
public class NoPublicServiceMethodsException extends Exception
{
    public NoPublicServiceMethodsException(Class class1)
    {
        super("No public service methods declared in " + class1 + " Did you forget the javax.jws.WebService annotation?");
    }
}
