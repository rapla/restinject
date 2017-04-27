package org.rapla.inject.generator.internal;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

public class SourceWriter
{
    private final PrintWriter printWriter;
    private int indent = 0;
    private boolean newLine = true;

    final Set<String> methodNames = new HashSet<String>();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    String packageName;
    String componentName;
    ProcessingEnvironment processingEnv;

    public SourceWriter(String packageName, String componentName, ProcessingEnvironment env)
    {
        this.processingEnv = env;
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
    
    public String getQualifiedName()
    {
        return packageName + "." + componentName;
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

    public void close() throws IOException
    {
        printWriter.close();
        final boolean generate;
        String componentName = getComponentName();
        String packageName = getPackageName();
        final byte[] bytes = toBytes();
        final Filer filer = processingEnv.getFiler();
        JavaFileManager.Location loc = StandardLocation.SOURCE_OUTPUT;
        final String key = packageName + "." + componentName;
        final FileObject resource = filer.getResource(loc, packageName, componentName + ".java");
        if (resource != null)
        {
            generate = true;
            //if (!resource.delete())
            {
//                byte[] bytesFromInputStream = null;
//                try (InputStream in = resource.openInputStream())
//                {
//                    bytesFromInputStream = getBytesFromInputStream(in);
//                }
//                catch (IOException ex)
//                {
//                }
//                if (bytesFromInputStream != null)
//                {
//                    processingEnv.getMessager().printMessage(Kind.WARNING, "Could not delete file " + packageName + "." + componentName + ".java" + " ModuleInformation maybe old. Please run a full rebuild to resolve.", null);
//                    //generate = true;
//                }
                //            }
                //            else
                //            {
                //                generate = true;
                //            }
            }
        }
        else
        {
            generate = true;
        }
        if (generate)
        {

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating  " + key);
            final JavaFileObject sourceFile = filer.createSourceFile(key);
            try (OutputStream outputStream = sourceFile.openOutputStream())
            {
                outputStream.write(bytes);
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Created " + toString());
        }

        newLine = true;
    }

    public boolean containsMethod(String methodName)
    {
        return methodNames.contains(methodName);
    }

    public void addMethod(String methodName)
    {
        methodNames.add(methodName);
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException
    {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();)
        {
            byte[] buffer = new byte[0xFFFF];

            for (int len; (len = is.read(buffer)) != -1;)
                os.write(buffer, 0, len);

            os.flush();

            return os.toByteArray();
        }
    }
    
    @Override
    public String toString()
    {
        return getQualifiedName();
    }
}
