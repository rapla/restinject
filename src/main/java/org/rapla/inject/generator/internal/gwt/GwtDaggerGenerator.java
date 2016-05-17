package org.rapla.inject.generator.internal.gwt;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.rapla.inject.internal.GeneratorUtil;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class GwtDaggerGenerator extends Generator
{

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException
    {
        String moduleName = null;
        String file = "moduleDescription";
        ClassLoader classLoader = getClass().getClassLoader();
        final InputStream resourceAsStream = classLoader.getResourceAsStream(file);
        if (resourceAsStream == null)
        {
            final String message = "Can't load module description file " + file;
            logger.log(Type.ERROR, message);
            throw new UnableToCompleteException();
        }
        else
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream)))
            {
                moduleName = reader.readLine();
            }
            catch(Exception e)
            {
                logger.log(Type.ERROR, e.getMessage(), e);
            }
        }
        if (moduleName == null || moduleName.trim().length() == 0)
        {
            final String message = "No module defined in " + file;
            logger.log(Type.ERROR, message);
            throw new UnableToCompleteException();
        }
        try
        {
            final String generatedFileName = typeName + "Generated";
            final int lastIndexOfPackage = moduleName.lastIndexOf(".");
            final JClassType type = context.getTypeOracle().getType(typeName);
            final SourceWriter sourceWriter = getSourceWriter(type, context, logger);
            if(sourceWriter == null)
            {
                return generatedFileName;
            }
            // e.g. org.rapla
            final String packageName = (lastIndexOfPackage > 0 ? moduleName.substring(0, lastIndexOfPackage + 1) : "") + "client.gwt.dagger";
            // e.g. Rapla
            final String artifactName = GeneratorUtil.firstCharUp(lastIndexOfPackage >= 0 ? moduleName.substring(lastIndexOfPackage + 1) : moduleName);
            final String daggerComponentClassNamme = packageName + ".Dagger" + artifactName + "GwtComponent";
            sourceWriter.println(type.getQualifiedSourceName() + " dagger = " + daggerComponentClassNamme+".create().get"+type.getSimpleSourceName()+"();");
            final JMethod[] methods = type.getMethods();
            for (JMethod jMethod : methods)
            {
                sourceWriter.println();
                final String methodName = jMethod.getName();
                final String qualifiedSourceName = jMethod.getReturnType().getQualifiedSourceName();
                boolean isVoid = "void".equalsIgnoreCase(qualifiedSourceName);
                sourceWriter.print("public " + qualifiedSourceName+ " " + methodName+"(");
                final JParameter[] parameters = jMethod.getParameters();
                final JType[] parameterTypes = jMethod.getParameterTypes();
                boolean first = true;
                for(int i = 0; i < parameters.length; i++)
                {
                    if(first)
                    {
                        first = false;
                    }
                    else
                    {
                        sourceWriter.print(", ");
                    }
                    sourceWriter.print(parameterTypes[i].getQualifiedSourceName());
                    sourceWriter.print(" ");
                    sourceWriter.print(parameters[i].getName());
                }
                sourceWriter.println("){");
                sourceWriter.indent();
                if(!isVoid)
                    sourceWriter.print("return ");
                sourceWriter.print("dagger."+methodName+"(");
                for(int i = 0; i < parameters.length; i++)
                {
                    if(first)
                    {
                        first = false;
                    }
                    else
                    {
                        sourceWriter.print(", ");
                    }
                    sourceWriter.print(parameters[i].getName());
                }
                sourceWriter.println(");");
                sourceWriter.outdent();
                sourceWriter.println("}");
            }
            sourceWriter.commit(logger);
            return generatedFileName;
        }
        catch(Exception e)
        {
            logger.log(Type.ERROR, e.getMessage(), e);
            return null;
        }
    }
    
    public SourceWriter getSourceWriter(JClassType classType, GeneratorContext context, TreeLogger logger)
    {
        String packageName = classType.getPackage().getName();
        String simpleName = classType.getSimpleSourceName() + "Generated";
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, simpleName);
        composer.addImplementedInterface(classType.getQualifiedSourceName());

        PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
        if (printWriter == null)
        {
            return null;
        }
        else
        {
            SourceWriter sw = composer.createSourceWriter(context, printWriter);
            return sw;
        }
    }

}
