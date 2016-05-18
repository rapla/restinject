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

        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        public boolean isTraceEnabled() {
            return logger.isTraceEnabled();
        }

        public void trace(String message) {
            log(Level.TRACE, message);
        }

        public void debug(String message) {
            log(Level.DEBUG, message);
        }

		public void info(String message) {
            log(Level.INFO, message);
        }

        private void log(Level infoInt, String message) {
        	log( infoInt, message, null);
		}

		private void log( Level level, String message,Throwable t) {
			String fqcn = Wrapper.class.getName();
            if ( logger instanceof ExtendedLogger)
            {
                ((ExtendedLogger)logger).logIfEnabled(fqcn,level, null,message,t);
            }
            else
            {
                logger.log(level, message, t);
            }
		}
        

        public void warn(String message) {
            log(Level.WARN, message);
        }

        public void warn(String message, Throwable cause) {
        	log( Level.WARN, message, cause);
        }

        public void error(String message) {
        	log( Level.ERROR, message);
        }

        public void error(String message, Throwable cause) {
        	log( Level.ERROR, message, cause);
        }

        public Logger getChildLogger(String childLoggerName)
        {
            String childId = id + "." + childLoggerName;
            return getLoggerForCategory( childId);
        }
        
    }


}
