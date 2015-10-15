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

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.rapla.gwtjsonrpc.rebind.SerializerClasses;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Creator of ResultDeserializers. Actually, only object arrays have created
 * deserializers:
 * All object arrays that have a JSONSerializer for the array component can be
 * generated, but they will need to live in the same package as the serializer.
 * To do this, if the serializer lives in the
 * <code>com.google.gwtjsonrpc.gwt</code> package (where custom object
 * serializers live), the ResultDeserializer for it's array will be placed in
 * this package as well. Else it will be placed with the serializer in the
 * package the object lives.
 */
class ResultDeserializerCreator
{
    private static final String DSER_SUFFIX = "_ResultDeserializer";

    private static final HashMap<String, String> generatedDeserializers = new HashMap<String, String>();

    private SerializerCreator serializerCreator;
    private final NameFactory nameFactory = new NameFactory();
    private final ProcessingEnvironment processingEnvironment;

    private TypeMirror targetType;
    private TypeMirror componentType;
    private final String generatorName;

    ResultDeserializerCreator(SerializerCreator sc, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        this.processingEnvironment = processingEnvironment;
        serializerCreator = sc;
        this.generatorName = generatorName;
    }

    void create(TreeLogger logger, TypeMirror targetType)
    {
        this.targetType = targetType;
        final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType);
        this.componentType = arrayType.getComponentType();

        if (SerializerCreator.isPrimitive(componentType) || SerializerCreator.isBoxedPrimitive(componentType))
        {
            logger.error("No need to create array deserializer for primitive array " + targetType);
            return;
        }

        if (deserializerFor(targetType) != null)
        {
            return;
        }

        logger.error("Creating result deserializer for " + asTypeElement(targetType).getSimpleName());
        final SourceWriter srcWriter = getSourceWriter(logger);
        if (srcWriter == null)
        {
            return;
        }
        final String dsn = getDeserializerQualifiedName(targetType);
        generatedDeserializers.put(asTypeElement(targetType).getQualifiedName().toString(), dsn);

        generateSingleton(srcWriter);
        generateInstanceMembers(srcWriter);
        generateFromResult(srcWriter);
        srcWriter.outdent();
        srcWriter.println("}");
        srcWriter.close();
    }
    
    private TypeElement asTypeElement(TypeMirror tm)
    {
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final Element asElement = typeUtils.asElement(tm);
        return (TypeElement) asElement;
    }

    private void generateSingleton(final SourceWriter w)
    {
        w.print("public static final ");
        w.print(getDeserializerSimpleName(targetType, nameFactory, processingEnvironment));
        w.print(" INSTANCE = new ");
        w.print(getDeserializerSimpleName(targetType, nameFactory, processingEnvironment));
        w.println("();");
        w.println();
    }

    private void generateInstanceMembers(SourceWriter w)
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

    private void generateFromResult(SourceWriter w)
    {
        final String ctn = componentType.toString();

        w.println("@Override");
        w.print("public " + ctn + "[] ");
        w.println("fromResult(JavaScriptObject responseObject) {");
        w.indent();
        w.print("final " + ctn + "[] tmp = new " + ctn);
        w.println("[getResultSize(responseObject)];");

        w.println("serializer.fromJson(getResult(responseObject), tmp);");
        w.println("return tmp;");
        w.outdent();
        w.println("}");
    }

    private String getDeserializerQualifiedName(TypeMirror targetType)
    {
        final String pkgName = getDeserializerPackageName(targetType);
        final String className = getDeserializerSimpleName(targetType, nameFactory, processingEnvironment);
        return pkgName.length() == 0 ? className : pkgName + "." + className;
    }

    private String getDeserializerPackageName(TypeMirror targetType)
    {
        // Place array deserializer in same package as the component deserializer
        final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType);
        final TypeMirror componentType = arrayType.getComponentType();
        final String compSerializer = serializerCreator.serializerFor(componentType);
        final int end = compSerializer.lastIndexOf('.');
        return end >= 0 ? compSerializer.substring(0, end) : "";
    }

    private static String getDeserializerSimpleName(TypeMirror targetType, NameFactory nameFactory, ProcessingEnvironment processingEnvironment)
    {
        final TypeElement typeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(targetType);
        return ProxyCreator.synthesizeTopLevelClassName(typeElement, DSER_SUFFIX, nameFactory, processingEnvironment)[1];
    }

    private SourceWriter getSourceWriter(TreeLogger logger)
    {
        String pkgName = getDeserializerPackageName(targetType);
        final String simpleName = getDeserializerSimpleName(targetType, nameFactory, processingEnvironment);
        SourceWriter pw;
        try
        {
            final JavaFileObject writer = processingEnvironment.getFiler().createSourceFile(pkgName + "." + simpleName);
            pw = new SourceWriter(writer.openWriter());
        }
        catch (IOException e)
        {
            return null;
        }
        pw.println("package " + pkgName + ";");
        pw.println("import " + JavaScriptObject.class.getCanonicalName() + ";");
        pw.println("import " + org.rapla.gwtjsonrpc.rebind.SerializerClasses.ResultDeserializer + ";");
        pw.println(getGeneratorString());
        pw.println("public class " + simpleName + " extends " + org.rapla.gwtjsonrpc.rebind.SerializerClasses.ArrayResultDeserializer + " implements "
                + org.rapla.gwtjsonrpc.rebind.SerializerClasses.ResultDeserializer + "<" + asTypeElement(targetType).getQualifiedName().toString() + ">");
        pw.println("{");
        pw.indent();
        return pw;
    }

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    private String deserializerFor(TypeMirror targetType)
    {
        final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType);
        TypeMirror componentType = arrayType.getComponentType();
        // Custom primitive deserializers
        if (SerializerCreator.isBoxedPrimitive(componentType))
            return org.rapla.gwtjsonrpc.rebind.SerializerClasses.PrimitiveArrayResultDeserializers + "."
                    + asTypeElement(componentType).getSimpleName().toString().toUpperCase() + "_INSTANCE";
        final TypeElement typeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(targetType);
        final String name = generatedDeserializers.get(typeElement.getQualifiedName().toString());

        return name == null ? null : name + ".INSTANCE";
    }

    public void generateDeserializerReference(TypeMirror targetType, SourceWriter w)
    {
        if (SerializerCreator.isBoxedPrimitive(targetType))
        {
            w.print(SerializerClasses.PrimitiveResultDeserializers);
            w.print(".");
            w.print(asTypeElement(targetType).getSimpleName().toString().toUpperCase());
            w.print("_INSTANCE");
        }
        else if (targetType instanceof ArrayType)
        {
            final ArrayType arrayType = processingEnvironment.getTypeUtils().getArrayType(targetType);
            w.print(deserializerFor(arrayType));
        }
        else
        {
            serializerCreator.generateSerializerReference(targetType, w, false);
        }
    }
}
