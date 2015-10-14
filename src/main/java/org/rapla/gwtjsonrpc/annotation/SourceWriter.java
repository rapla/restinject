package org.rapla.gwtjsonrpc.annotation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

public class SourceWriter
{
    private final PrintWriter printWriter;
    private int indent = 0;
    private boolean newLine = true;

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


}
