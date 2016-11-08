package org.rapla.logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Provider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AbstractLoggerTest
{
    File file;
    Logger logger;
    @Before
    public void setUp() throws IOException
    {
        try
        {
            File folder = new File("target/temp/");
            folder.mkdirs();
            file = new File(folder, getLogFileName());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        final Provider<Logger> log4jAdapter = getLogAdapter();
        logger = log4jAdapter.get();
    }

    protected abstract Provider<Logger> getLogAdapter() throws IOException;
    protected abstract String getLogFileName();


    @After
    public void tearDown()
    {
//        if (file != null)
//        {
//            final boolean delete = file.delete();
//            System.out.println("Deleted " + file + " after test = " + delete);
//        }
    }

    public static void testLine(File file,String message,String methodName)
    {
        boolean containsMessage = false;
        boolean containsMethod = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {
            while (true)
            {
                final String line = reader.readLine();
                if ( line == null)
                {
                    break;
                }
                {
                    if (line.contains(message))
                    {
                        containsMessage = true;
                    }
                }
                {
                    if (line.contains(methodName))
                    {
                        containsMethod = true;
                    }
                }
            }

        }
        catch (Exception ex)
        {
            Assert.fail("Message \"" +message + "\" not logged in " + methodName );
        }
        if (!containsMessage)
        {
            Assert.fail("Message \"" + message + "\" not logged in " + methodName);
        }
        if (!containsMethod)
        {
            Assert.fail("Methodname " + methodName + " not logged");
        }
    }



    @Test public void testLog()
    {
        final String message = "Test Info";
        logger.info(message);
        final String methodName = "testLog";
        testLine(file,message, methodName);
    }

    @Test public void testDebug()
    {
        final String message = "Test Debug";
        if ( logger.isDebugEnabled())
        {
            logger.debug(message);
        }
        final String methodName = "testDebug";
        testLine(file,message, methodName);
    }

    @Test public void testTrace()
    {
        final String message = "Test Trace";
        if ( logger.isTraceEnabled())
        {
            logger.trace(message);
        }
        final String methodName = "testTrace";
        testLine(file,message, methodName);
    }



}
