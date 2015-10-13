// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.rapla.gwtjsonrpc.annotation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;

import org.rapla.gwtjsonrpc.rebind.SerializerClasses;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Creator of ResultDeserializers. Actually, only object arrays have created
 * deserializers:
 * All object arrays that have a JSONSerializer for the array component can be
 * generated, but they will need to live in the same package as the serializer.
 * To do this, if the serializer lives in the
 * <code>com.google.gwtjsonrpc.client</code> package (where custom object
 * serializers live), the ResultDeserializer for it's array will be placed in
 * this package as well. Else it will be placed with the serializer in the
 * package the object lives.
 */
class ResultDeserializerCreator
{
    private static final String DSER_SUFFIX = "_ResultDeserializer";

    private HashMap<String, String> generatedDeserializers;
    private SerializerCreator serializerCreator;
    private final NameFactory nameFactory = new NameFactory();
    private final ProcessingEnvironment processingEnvironment;

    private TypeElement targetType;
    private TypeElement componentType;

    ResultDeserializerCreator(SerializerCreator sc, ProcessingEnvironment processingEnvironment)
    {
        this.processingEnvironment = processingEnvironment;
        generatedDeserializers = new HashMap<String, String>();
        serializerCreator = sc;
    }

    void create(TreeLogger logger, TypeElement targetType)
    {
        this.targetType = targetType;
        final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType.asType());
        this.componentType = (TypeElement) processingEnvironment.getTypeUtils().asElement(arrayType.getComponentType());

        if (SerializerCreator.isPrimitive(componentType) || SerializerCreator.isBoxedPrimitive(componentType))
        {
            logger.error("No need to create array deserializer for primitive array " + targetType);
            return;
        }

        if (deserializerFor(targetType) != null)
        {
            return;
        }

        logger.error("Creating result deserializer for " + targetType.getSimpleName());
        final PrintWriter srcWriter = getSourceWriter(logger);
        if (srcWriter == null)
        {
            return;
        }
        final String dsn = getDeserializerQualifiedName(targetType);
        generatedDeserializers.put(targetType.getQualifiedName().toString(), dsn);

        generateSingleton(srcWriter);
        generateInstanceMembers(srcWriter);
        generateFromResult(srcWriter);

        srcWriter.close();
    }

    private void generateSingleton(final PrintWriter w)
    {
        w.print("public static final ");
        w.print(getDeserializerSimpleName(targetType, nameFactory, processingEnvironment));
        w.print(" INSTANCE = new ");
        w.print(getDeserializerSimpleName(targetType, nameFactory, processingEnvironment));
        w.println("();");
        w.println();
    }

    private void generateInstanceMembers(PrintWriter w)
    {
        w.print("private final ");
        w.print(serializerCreator.serializerFor(targetType));
        w.print(" ");
        w.print("serializer");
        w.print(" = ");
        serializerCreator.generateSerializerReference(targetType, w, true);
        w.println(";");
        w.println();
    }

    private void generateFromResult(PrintWriter w)
    {
        final String ctn = componentType.getQualifiedName().toString();

        w.println("@Override");
        w.print("public " + ctn + "[] ");
        w.println("fromResult(JavaScriptObject responseObject) {");

        w.print("final " + ctn + "[] tmp = new " + ctn);
        w.println("[getResultSize(responseObject)];");

        w.println("serializer.fromJson(getResult(responseObject), tmp);");
        w.println("return tmp;");

        w.println("}");
    }

    private String getDeserializerQualifiedName(TypeElement targetType)
    {
        final String pkgName = getDeserializerPackageName(targetType);
        final String className = getDeserializerSimpleName(targetType, nameFactory, processingEnvironment);
        return pkgName.length() == 0 ? className : pkgName + "." + className;
    }

    private String getDeserializerPackageName(TypeElement targetType)
    {
        // Place array deserializer in same package as the component deserializer
        final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType.asType());
        final TypeElement ct = (TypeElement) processingEnvironment.getTypeUtils().asElement(arrayType.getComponentType());
        final String compSerializer = serializerCreator.serializerFor(ct);
        final int end = compSerializer.lastIndexOf('.');
        return end >= 0 ? compSerializer.substring(0, end) : "";
    }

    private static String getDeserializerSimpleName(TypeElement targetType, NameFactory nameFactory, ProcessingEnvironment processingEnvironment)
    {
        return ProxyCreator.synthesizeTopLevelClassName(targetType, DSER_SUFFIX, nameFactory, processingEnvironment)[1];
    }

    private PrintWriter getSourceWriter(TreeLogger logger)
    {
        String pkgName = getDeserializerPackageName(targetType);
        final String simpleName = getDeserializerSimpleName(targetType, nameFactory, processingEnvironment);
        PrintWriter pw;
        try
        {
            pw = new PrintWriter(new FileOutputStream(new File(pkgName + "/" + simpleName)));
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
        pw.println("package " + pkgName + ";");
        pw.println("import " + JavaScriptObject.class.getCanonicalName() + ";");
        pw.println("import " + org.rapla.gwtjsonrpc.rebind.SerializerClasses.ResultDeserializer + ";");
        pw.println("public class " + simpleName + " extends " + org.rapla.gwtjsonrpc.rebind.SerializerClasses.ArrayResultDeserializer + " implements "
                + org.rapla.gwtjsonrpc.rebind.SerializerClasses.ResultDeserializer + "<" + targetType.getQualifiedName().toString() + ">{");
        return pw;
    }

    private String deserializerFor(TypeElement targetType)
    {
        final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType.asType());
        TypeElement componentType = (TypeElement) processingEnvironment.getTypeUtils().asElement(arrayType.getComponentType());
        // Custom primitive deserializers
        if (SerializerCreator.isBoxedPrimitive(componentType))
            return org.rapla.gwtjsonrpc.rebind.SerializerClasses.PrimitiveArrayResultDeserializers + "."
                    + componentType.getSimpleName().toString().toUpperCase() + "_INSTANCE";
        final String name = generatedDeserializers.get(targetType.getQualifiedName().toString());

        return name == null ? null : name + ".INSTANCE";
    }

    public void generateDeserializerReference(TypeElement targetType, PrintWriter w)
    {
        if (SerializerCreator.isBoxedPrimitive(targetType))
        {
            w.print(SerializerClasses.PrimitiveResultDeserializers);
            w.print(".");
            w.print(targetType.getSimpleName().toString().toUpperCase());
            w.print("_INSTANCE");
        }
        else if (targetType.asType() instanceof ArrayType)
        {
            final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType.asType());
            final TypeElement target = (TypeElement) processingEnvironment.getTypeUtils().asElement(arrayType);
            w.print(deserializerFor(target));
        }
        else
        {
            serializerCreator.generateSerializerReference(targetType, w, false);
        }
    }
}
