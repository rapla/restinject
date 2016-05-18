/*--------------------------------------------------------------------------*
| Copyright (C) 2013 Christopher Kohlhaas                                  |
|                                                                          |
| This program is free software; you can redistribute it and/or modify     |
| it under the terms of the GNU General Public License as published by the |
| Free Software Foundation. A copy of the license has been included with   |
| these distribution in the COPYING file, if not go to www.fsf.org         |
|                                                                          |
| As a special exception, you are granted the permissions to link this     |
| program with every library, which license fulfills the Open Source       |
| Definition as published by the Open Source Initiative (OSI).             |
*--------------------------------------------------------------------------*/
package org.rapla.logger.internal;


import javax.inject.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaplaJDKLoggingAdapter implements Provider<org.rapla.logger.Logger>
{
    private static String WRAPPER_NAME = RaplaJDKLoggingAdapter.class.getName();

    AbstractJDKLogger abstractJDKLogger;

    public org.rapla.logger.Logger get() {
        if ( abstractJDKLogger == null)
        {
            synchronized ( this) {
                if ( abstractJDKLogger == null)
                {
                    Logger logger = Logger.getLogger(  "rapla");
                    abstractJDKLogger = new JDKLogger(logger, "rapla");
                }
            }
        }
        return abstractJDKLogger;
    }

    static protected class JDKLogger extends AbstractJDKLogger
    {
        public JDKLogger(Logger logger, String rapla)
        {
            super(logger, rapla);
        }

        @Override
        protected void log_(Logger logger, Level level, String message, Throwable cause) {
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            String sourceClass = null;
            String sourceMethod =null;
            for (StackTraceElement element:stackTrace)
            {
                String classname = element.getClassName();
                if ( !classname.startsWith(WRAPPER_NAME))
                {
                    sourceClass=classname;
                    sourceMethod =element.getMethodName();
                    break;
                }
            }
            logger.logp(level,sourceClass, sourceMethod,message, cause);
        }

        @Override protected org.rapla.logger.Logger createChildLogger(String childId)
        {
            Logger childLogger = Logger.getLogger(childId);
            return new JDKLogger( childLogger, childId);
        }


    }



}
