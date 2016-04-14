package org.rapla.rest.client;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SerializableExceptionInformation
{

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SerializableExceptionStacktraceInformation
    {
        private String className;
        private String methodName;
        private int lineNumber;
        private String fileName;

        public SerializableExceptionStacktraceInformation()
        {
        }
        
        public SerializableExceptionStacktraceInformation(String className, String methodName, int lineNumber, String fileName)
        {
            super();
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.fileName = fileName;
        }

        public SerializableExceptionStacktraceInformation(StackTraceElement stackTraceElement)
        {
            className = stackTraceElement.getClassName();
            methodName = stackTraceElement.getMethodName();
            lineNumber = stackTraceElement.getLineNumber();
            fileName = stackTraceElement.getFileName();
        }

        public String getClassName()
        {
            return className;
        }

        public String getMethodName()
        {
            return methodName;
        }

        public int getLineNumber()
        {
            return lineNumber;
        }

        public String getFileName()
        {
            return fileName;
        }
    }

    private String message;
    private String exceptionClass;
    private ArrayList<String> messages;
    private ArrayList<SerializableExceptionStacktraceInformation> stacktrace;

    public SerializableExceptionInformation()
    {
    }

    public SerializableExceptionInformation(Throwable t)
    {
        this.message = t.getMessage();
        this.messages = null;
        this.stacktrace = new ArrayList<>();
        this.exceptionClass = t.getClass().getCanonicalName();
        final StackTraceElement[] stackTrace2 = t.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace2)
        {
            stacktrace.add(new SerializableExceptionStacktraceInformation(stackTraceElement));
        }
    }

    public SerializableExceptionInformation(String message, String exceptionClass, ArrayList<String> messages, ArrayList<SerializableExceptionStacktraceInformation> stacktrace)
    {
        this.message = message;
        this.exceptionClass = exceptionClass;
        this.messages = messages;
        this.stacktrace = stacktrace;
    }
    
    public String getExceptionClass()
    {
        return exceptionClass;
    }

    public String getMessage()
    {
        return message;
    }

    public ArrayList<String> getMessages()
    {
        return messages;
    }

    public ArrayList<SerializableExceptionStacktraceInformation> getStacktrace()
    {
        return stacktrace;
    }

}
