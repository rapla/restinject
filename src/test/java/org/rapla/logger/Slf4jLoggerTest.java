package org.rapla.logger;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.internal.Log4jAdapter;
import org.rapla.logger.internal.Slf4jAdapter;

import javax.inject.Provider;

@RunWith(JUnit4.class) public class Slf4jLoggerTest extends AbstractLoggerTest
{
    @Override protected Provider<Logger> getLogAdapter()
    {
        return new Slf4jAdapter();
    }

    @Override protected String getLogFileName()
    {
        return "slf4j.log";
    }
}

