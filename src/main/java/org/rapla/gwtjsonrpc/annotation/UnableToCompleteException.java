package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.client.ExceptionDeserializer;

/**
 * Created by Christopher on 13.10.2015.
 */
public class UnableToCompleteException extends Exception
{
    public UnableToCompleteException(String message)
    {
        super(message);
    }
    public UnableToCompleteException(Exception ex)
    {
        super(ex);
    }
}
