package org.rapla.rest.generator.internal;

public class UnableToCompleteException extends Exception
{
    public UnableToCompleteException(String message)
    {
        super(message);
    }

    public UnableToCompleteException(String message, Exception ex)
    {
        super(message,ex);
    }
    public UnableToCompleteException(Exception ex)
    {
        this(ex.getMessage(),ex);
    }
}
