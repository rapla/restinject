package org.rapla.logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.internal.RaplaJDKLoggingAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.LogManager;

@RunWith(JUnit4.class) public class JDKLoggerTest
{
    File folder;
    @Before
    public void setUp()
    {
        try
        {
            folder = new File("target/temp");
            folder.mkdirs();
            InputStream stream = JDKLoggerTest.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(stream);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private  void testLine(String message,String methodName)
    {
        final File file = new File(folder, "rapla.log");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {
            {
                final String firstLine = reader.readLine();
                if (!firstLine.contains(methodName))
                {
                    Assert.fail("Methodname " + methodName + " not logged");
                }
            }
            {
                final String secondLine = reader.readLine();
                if (!secondLine.contains(message))
                {
                    Assert.fail("Message \"" +message + "\"not logged in " + methodName );
                }
            }
        }
        catch (Exception ex)
        {
            Assert.fail("Message \"" +message + "\"not logged in " + methodName );
        }
    }

    @Test public void testLog()
    {
        //System.setProperty( "java.util.logging.config.file", "logging.properties" );

        final RaplaJDKLoggingAdapter raplaJDKLoggingAdapter = new RaplaJDKLoggingAdapter();
        final Logger logger = raplaJDKLoggingAdapter.get();
        final String message = "Test Info";
        logger.info(message);
        final String methodName = "testLog";
        testLine(message, methodName);
    }

    @Test public void testDebug()
    {
        //System.setProperty( "java.util.logging.config.file", "logging.properties" );

        final RaplaJDKLoggingAdapter raplaJDKLoggingAdapter = new RaplaJDKLoggingAdapter();
        final Logger logger = raplaJDKLoggingAdapter.get();
        final String message = "Test Debug";
        if ( logger.isDebugEnabled())
        {
            logger.debug(message);
        }
        final String methodName = "testDebug";
        testLine(message, methodName);
    }

    @Test public void testTrace()
    {
        //System.setProperty( "java.util.logging.config.file", "logging.properties" );

        final RaplaJDKLoggingAdapter raplaJDKLoggingAdapter = new RaplaJDKLoggingAdapter();
        final Logger logger = raplaJDKLoggingAdapter.get();
        final String message = "Test Trace";
        if ( logger.isTraceEnabled())
        {
            logger.trace(message);
        }
        final String methodName = "testTrace";
        testLine(message, methodName);
    }



}
