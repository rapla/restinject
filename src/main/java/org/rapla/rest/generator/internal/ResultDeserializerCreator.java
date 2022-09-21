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

package org.rapla.rest.generator.internal;

import java.io.IOException;
import java.util.HashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.rapla.inject.generator.internal.SourceWriter;

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
public class ResultDeserializerCreator
{
    private static final String DSER_SUFFIX = "_ResultDeserializer";

    private static final HashMap<String, String> generatedDeserializers = new HashMap<String, String>();

    private SerializerCreator serializerCreator;
    private final ProcessingEnvironment processingEnvironment;

    private TypeMirror targetType;
    private TypeMirror componentType;
    private final String generatorName;

    public ResultDeserializerCreator(SerializerCreator sc, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        this.processingEnvironment = processingEnvironment;
        serializerCreator = sc;
        this.generatorName = generatorName;
    }

    void create(TreeLogger logger, TypeMirror targetType) throws IOException
    {
        this.targetType = targetType;
        final ArrayType arrayType = (ArrayType)targetType;//processingEnvironment.getTypeUtils().getArrayType(targetType);
        this.componentType = arrayType.getComponentType();

        if (SerializerCreator.isPrimitive(componentType) || SerializerCreator.isBoxedPrimitive(componentType))
        {
            logger.error("No need to create array deserializer for primitive array " + targetType);
            return;
        }

        if (deserializerFor(arrayType) != null)
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
        w.print(getDeserializerSimpleName(targetType, processingEnvironment));
        w.print(" INSTANCE = new ");
        w.print(getDeserializerSimpleName(targetType, processingEnvironment));
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
        w.println("fromResult(Object responseObject) {");
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
        final String className = getDeserializerSimpleName(targetType, processingEnvironment);
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

    private static String getDeserializerSimpleName(TypeMirror targetType, ProcessingEnvironment processingEnvironment)
    {
        final TypeElement typeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(targetType);
        return AbstractClientProxyCreator.synthesizeTopLevelClassName(typeElement, DSER_SUFFIX, processingEnvironment)[1];
    }

    private SourceWriter getSourceWriter(TreeLogger logger)
    {
        String pkgName = getDeserializerPackageName(targetType);
        final String simpleName = getDeserializerSimpleName(targetType, processingEnvironment);
        SourceWriter pw = new SourceWriter(pkgName, simpleName, processingEnvironment);
        pw.println("package " + pkgName + ";");
        pw.println("import " + SerializerClasses.ResultDeserializer + ";");
        pw.println(getGeneratorString());
        pw.println("public class " + simpleName + " extends " + SerializerClasses.ArrayResultDeserializer + " implements "
                + SerializerClasses.ResultDeserializer + "<" + asTypeElement(targetType).getQualifiedName().toString() + ">");
        pw.println("{");
        pw.indent();
        return pw;
    }

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    private String deserializerFor(ArrayType arrayType)
    {
        TypeMirror componentType = arrayType.getComponentType();
        // Custom primitive deserializers
        if (SerializerCreator.isBoxedPrimitive(componentType))
            return SerializerClasses.PrimitiveArrayResultDeserializers + "."
                    + asTypeElement(componentType).getSimpleName().toString().toUpperCase() + "_INSTANCE";
        if(SerializerCreator.isPrimitive(componentType))
            return SerializerClasses.PrimitiveArrayResultDeserializers + "."
                    + getPrimitiveSerializerName(componentType) + "_INSTANCE";
        final TypeElement typeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(targetType);
        final String name = generatedDeserializers.get(typeElement.getQualifiedName().toString());

        return name == null ? null : name + ".INSTANCE";
    }

    public void generateDeserializerReference(TypeMirror targetType, SourceWriter w)
    {
        if (SerializerCreator.isBoxedPrimitive(targetType) || SerializerCreator.isPrimitive(targetType))
        {
            w.print("null");
//            w.print(SerializerClasses.PrimitiveResultDeserializers);
//            w.print(".");
//            final String serializerName;
//            final TypeElement typeElement = asTypeElement(targetType);
//            if (typeElement != null)
//            {
//                serializerName = typeElement.getSimpleName().toString().toUpperCase();
//            }
//            else
//            {
//                serializerName = getPrimitiveSerializerName(targetType);
//
//            }
//            w.print(serializerName);
//            w.print("_INSTANCE");
        }
        else if (targetType instanceof ArrayType)
        {
            final ArrayType arrayType = (ArrayType)targetType;
            w.print(deserializerFor(arrayType));
        }
        else
        {
            serializerCreator.generateSerializerReference(targetType, w, false);
        }
    }

    private String getPrimitiveSerializerName(TypeMirror targetType)
    {
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) targetType);
        final Name simpleName = boxedClass.getSimpleName();
        final String string = simpleName.toString();
        final String serializerName = string.toUpperCase();
        return serializerName;
    }
}
