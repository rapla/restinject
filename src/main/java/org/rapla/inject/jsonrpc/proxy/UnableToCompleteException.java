package org.rapla.inject.jsonrpc.proxy;

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
