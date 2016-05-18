package org.rapla.logger;


public interface Logger {
    boolean isTraceEnabled();
    boolean isDebugEnabled();
    void debug(String message);
    void info(String message);
    void warn(String message);
    void warn(String message, Throwable cause);
    void error(String message);
    void error(String message, Throwable cause);
    void trace(String message);
    Logger getChildLogger(String childLoggerName);

}
