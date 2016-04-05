package org.rapla.rest.client.swing;

public class RaplaConnectException extends Exception {

    private static final long serialVersionUID = 1L;

    public RaplaConnectException()
    {
        super("Error to connect");
    }
    public RaplaConnectException(String text) {
        this(text, null);
    }
    
    public RaplaConnectException(Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public RaplaConnectException(String message,Throwable cause) {
        super(message, cause);
    }


}
