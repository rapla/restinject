package org.rapla.logger;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.internal.Log4jAdapter;

import javax.inject.Provider;

@RunWith(JUnit4.class) public class Log4jLoggerTest extends AbstractLoggerTest
{
    @Override protected Provider<Logger> getLogAdapter()
    {
        return new Log4jAdapter();
    }

    @Override protected String getLogFileName()
    {
        return "log4j.log";
    }
}

