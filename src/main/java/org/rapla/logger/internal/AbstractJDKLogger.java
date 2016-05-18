package org.rapla.logger.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

abstract class AbstractJDKLogger implements org.rapla.logger.Logger
{
    Logger logger;
    String id;

    public AbstractJDKLogger(Logger logger, String id)
    {
        this.logger = logger;
        this.id = id;
    }

    public boolean isTraceEnabled()
    {
        return logger.isLoggable(Level.FINEST);
    }

    public boolean isDebugEnabled()
    {
        return logger.isLoggable(Level.CONFIG);
    }

    public void trace(String message)
    {
        log(Level.FINEST, message);
    }

    public void debug(String message)
    {
        log(Level.CONFIG, message);
    }

    public void info(String message)
    {
        log(Level.INFO, message);
    }

    public void warn(String message)
    {
        log(Level.WARNING, message);
    }

    public void warn(String message, Throwable cause)
    {
        log(Level.WARNING, message, cause);
    }

    public void error(String message)
    {
        log(Level.SEVERE, message);
    }

    public void error(String message, Throwable cause)
    {
        log(Level.SEVERE, message, cause);
    }

    private void log(Level level, String message)
    {
        log(level, message, null);
    }

    abstract protected void log(Level level, String message, Throwable cause);


    public org.rapla.logger.Logger getChildLogger(String childLoggerName)
    {
        String childId = id + "." + childLoggerName;
        return createChildLogger(childId);
    }

    abstract protected org.rapla.logger.Logger createChildLogger(String childId);


}
