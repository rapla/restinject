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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.rapla.logger.Logger;

import javax.inject.Provider;

@SuppressWarnings("restriction")

public class Log4jAdapter implements Provider<Logger> {

    static public Logger getLoggerForCategory(String categoryName) {
        
        org.apache.logging.log4j.Logger loggerForCategory = LogManager.getLogger(categoryName);
        return new Wrapper(loggerForCategory, categoryName);
    }

    public Logger get() {
        return getLoggerForCategory( "rapla");
    }
    
    static class Wrapper implements Logger
    {
        org.apache.logging.log4j.Logger logger;
        String id;

        public Wrapper( org.apache.logging.log4j.Logger loggerForCategory, String id) {
            this.logger = loggerForCategory;

            this.id = id;
        }

        public Boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        public Boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        public Void trace(String message) {
            return log(Level.TRACE, message);
        }

        public Void debug(String message) {
            return log(Level.DEBUG, message);
        }

		public Void info(String message) {
            return log(Level.INFO, message);
        }

        private Void log(Level infoInt, String message) {
        	return log( infoInt, message, null);
		}

		private Void log( Level level, String message,Throwable t) {
			String fqcn = Wrapper.class.getName();
            if ( logger instanceof ExtendedLogger)
            {
                ((ExtendedLogger)logger).logIfEnabled(fqcn,level, null,message,t);
            }
            else
            {
                logger.log(level, message, t);
            }
            return null;
		}
        

        public Void warn(String message) {
            return log(Level.WARN, message);
        }

        public Void warn(String message, Throwable cause) {
        	return log( Level.WARN, message, cause);
        }

        public Void error(String message) {
        	return log( Level.ERROR, message);
        }

        public Void error(String message, Throwable cause) {
        	return log( Level.ERROR, message, cause);
        }

        public Logger getChildLogger(String childLoggerName)
        {
            String childId = id + "." + childLoggerName;
            return getLoggerForCategory( childId);
        }
        
    }


}
