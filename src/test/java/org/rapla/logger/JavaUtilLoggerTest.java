package org.rapla.logger;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.internal.JavaUtilLoggingAdapter;

import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

@RunWith(JUnit4.class) public class JavaUtilLoggerTest extends AbstractLoggerTest
{

    @Override protected Provider<Logger> getLogAdapter() throws IOException
    {
        InputStream stream = JavaUtilLoggerTest.class.getResourceAsStream("/logging.properties");
        LogManager.getLogManager().readConfiguration(stream);
        return new JavaUtilLoggingAdapter();
    }

    @Override protected String getLogFileName()
    {
        return "jul.log";
    }
}
