package org.rapla.rest.generator.internal;

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
