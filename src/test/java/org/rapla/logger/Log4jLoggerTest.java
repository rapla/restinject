package org.rapla.logger;

import org.jboss.resteasy.logging.impl.Log4jLogger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rapla.logger.internal.JavaUtilLoggingAdapter;
import org.rapla.logger.internal.Log4jAdapter;

import javax.inject.Provider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

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

