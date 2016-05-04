package org.rapla.inject.generator.internal;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class SourceWriter
{
    private final PrintWriter printWriter;
    private int indent = 0;
    private boolean newLine = true;

    final Set<String> methodNames = new HashSet<String>();
    public SourceWriter(Writer writer)
    {
        printWriter = new PrintWriter(writer);
    }

    public SourceWriter(OutputStream os)
    {
        printWriter = new PrintWriter(os);
    }

    public void indent()
    {
        indent++;
    }

    public void outdent()
    {
        indent--;
    }

    public void println(String content)
    {
        writeIndent();
        printWriter.println(content);
        newLine = true;
    }

    public void println()
    {
        printWriter.println();
    }

    private void writeIndent()
    {
        if (newLine)
        {
            for (int i = 0; i < indent; i++)
            {
                printWriter.print("  ");
            }
        }
    }

    public void print(String content)
    {
        writeIndent();
        printWriter.print(content);
        newLine = false;
    }

    public void close()
    {
        printWriter.close();
        newLine = true;
    }

    public boolean containsMethod(String methodName)
    {
        return methodNames.contains( methodName);
    }

    public void addMethod(String methodName)
    {
        methodNames.add( methodName);
    }
}
