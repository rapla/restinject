package org.rapla.inject.generator.internal;

import java.io.ByteArrayOutputStream;
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
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    String packageName;
    String componentName;

    public SourceWriter(String packageName,String componentName)
    {
        this.packageName = packageName;
        this.componentName = componentName;
        printWriter = new PrintWriter(stream);
    }

    public byte[] toBytes()
    {
        return stream.toByteArray();
    }

    public String getComponentName()
    {
        return componentName;
    }

    public String getPackageName()
    {
        return packageName;
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
