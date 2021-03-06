// Copyright 2008 Google Inc.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.rapla.inject.generator.internal.SourceWriter;

public class SerializerCreator implements SerializerClasses
{

    private final ProcessingEnvironment processingEnvironment;
    private static final String SER_SUFFIX = "_JsonSerializer";
    private static final Comparator<Element> FIELD_COMP = new Comparator<Element>()
    {
        @Override
        public int compare(final Element o1, final Element o2)
        {
            return o1.getSimpleName().toString().compareTo(o2.getSimpleName().toString());
        }
    };

    private final HashMap<String, String> defaultSerializers;
    private final HashMap<String, String> parameterizedSerializers;
    private final HashMap<String, String> generatedSerializers;
    private final String generatorName;

    public SerializerCreator(ProcessingEnvironment processingEnvironment, String generatorName)
    {
        this.processingEnvironment = processingEnvironment;
        this.generatorName = generatorName;
        defaultSerializers = new HashMap<String, String>();
        parameterizedSerializers = new HashMap<String, String>();

        defaultSerializers.put(String.class.getCanonicalName(), JavaLangString_JsonSerializer);
        defaultSerializers.put(Integer.class.getCanonicalName(), JavaLangInteger_JsonSerializer);
        defaultSerializers.put(Date.class.getCanonicalName(), JavaUtilDate_JsonSerializer);
        parameterizedSerializers.put(List.class.getCanonicalName(), ListSerializer);
        parameterizedSerializers.put(Map.class.getCanonicalName(), ObjectMapSerializer);
        parameterizedSerializers.put(Set.class.getCanonicalName(), SetSerializer);
        parameterizedSerializers.put(SortedSet.class.getCanonicalName(), SetSerializer);
        parameterizedSerializers.put(Collection.class.getCanonicalName(), CollectionSerializer);
        generatedSerializers = new HashMap<String, String>();
    }

    String create(final TypeMirror type, final TreeLogger logger) throws UnableToCompleteException
    {
        try
        {
            if (isParameterized(type) || isArray(type))
            {
                ensureSerializersForTypeParameters(logger, type);
            }
            String sClassName = serializerFor(type);
            if (sClassName != null)
            {
                return sClassName;
            }

            checkCanSerialize(logger, type, true);

            TypeElement targetType = (TypeElement) processingEnvironment.getTypeUtils().asElement(type);
            recursivelyCreateSerializers(logger, type);
            final String sn = getSerializerQualifiedName(targetType);
            //final TypeMirror erasedType = getErasedType(type, processingEnvironment);
            final String qname = targetType != null ? targetType.getQualifiedName().toString() : type.toString();
            if (!generatedSerializers.containsKey(qname))
            {
                generatedSerializers.put(qname, sn);
            }
            else
            {
                return sn;
            }
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "SerializerCreator: Creating Serializer for " + qname);
            final SourceWriter srcWriter = getSourceWriter( targetType);

            if (!isAbstract(type))
            {
                generateSingleton(srcWriter, targetType);
            }
            if (isEnum(type))
            {
                generateEnumFromJson(srcWriter, targetType);
            }
            else
            {
                generateInstanceMembers(srcWriter, targetType);
                generatePrintJson(srcWriter, targetType);
                generateFromJson(srcWriter, targetType);
                generateGetSets(srcWriter, targetType);
            }
            srcWriter.outdent();
            srcWriter.println("}");
            srcWriter.close();
            return sn;
        }
        catch (IOException ex)
        {
            throw new UnableToCompleteException(ex);
        }
    }

    private void recursivelyCreateSerializers(final TreeLogger logger, final TypeMirror targetType) throws UnableToCompleteException, IOException
    {
        if (isPrimitive(targetType) || isBoxedPrimitive(targetType) || targetType.getKind() == TypeKind.VOID)
        {
            return;
        }

        final TypeElement targetClass = (TypeElement) processingEnvironment.getTypeUtils().asElement(targetType);
        if (needsSuperSerializer(targetClass))
        {
            final TypeMirror superclass = targetClass.getSuperclass();
            create(superclass, logger);
        }

        for (final VariableElement f : sortFields(targetClass))
        {
            ensureSerializer(logger, f.asType());
        }
    }

    Set<TypeMirror> createdType = new HashSet<TypeMirror>();

    private void ensureSerializer(final TreeLogger logger, final TypeMirror type) throws UnableToCompleteException, IOException
    {
        if (ensureSerializersForTypeParameters(logger, type))
        {
            return;
        }

        final TypeMirror erasedType = getErasedType(type, processingEnvironment);
        final String qsn = erasedTypeString(erasedType, processingEnvironment);
        if (defaultSerializers.containsKey(qsn) || parameterizedSerializers.containsKey(qsn))
        {
            return;
        }
        if (!isClass(type) && !isEnum(type))
        {
            throw new UnableToCompleteException("type " + type + " is not a class");
        }
        if (createdType.contains(type))
        {
            return;
        }
        createdType.add(type);
        create(type, logger);
    }

    private boolean ensureSerializersForTypeParameters(final TreeLogger logger, TypeMirror type) throws UnableToCompleteException, IOException
    {
        if (isJsonPrimitive(type) || isBoxedPrimitive(type))
        {
            return true;
        }

        if (isArray(type))
        {
            ensureSerializer(logger, getArrayType(type));
            return true;
        }

        if (isParameterized(type))
        {
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) type).getTypeArguments();
            for (final TypeMirror typeMirror : typeArguments)
            {
                ensureSerializer(logger, typeMirror);
            }
        }

        return false;
    }

    void checkCanSerialize(final TreeLogger logger, final TypeMirror type) throws UnableToCompleteException
    {
        checkCanSerialize(logger, type, false);
    }

    Set<TypeMirror> checkedType = new HashSet<TypeMirror>();

    void checkCanSerialize(final TreeLogger logger, final TypeMirror type, boolean allowAbstractType) throws UnableToCompleteException
    {
        if (isPrimitiveLong(type))
        {
            logger.error("Type 'long' not supported in JSON encoding");
            throw new UnableToCompleteException("Type 'long' not supported in JSON encoding");
        }

        if (isEnum(type))
        {
            return;
        }
        if ( isVoid( type))
        {
            return;
        }

        if (isJsonPrimitive(type) || isBoxedPrimitive(type))
        {
            return;
        }

        if (isArray(type))
        {
            // FIXME need to check the leaf
            final TypeMirror leafType = getArrayType(type);
            if (isPrimitive(leafType) || isBoxedPrimitive(leafType))
            {
                ArrayType arrayType = ((ArrayType) type);
                if (GwtProxyCreator.getRank(arrayType) != 1)
                {
                    logger.error("restinject does not support " + "(de)serializing of multi-dimensional arrays of primitves");
                    // To work around this, we would need to generate serializers for
                    // them, this can be considered a todo
                    throw new UnableToCompleteException("restinject does not support " + "(de)serializing of multi-dimensional arrays of primitves");
                }
                else
                    // Rank 1 arrays work fine.
                    return;
            }
            checkCanSerialize(logger, getArrayType(type));
            return;
        }

        final String qsn = type.toString();
        if (defaultSerializers.containsKey(qsn))
        {
            return;
        }
        if (isParameterized(type))
        {
            List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
            // check if parameter types can be serialized
            for (final TypeMirror t : typeArgs)
            {
                checkCanSerialize(logger, t);
            }
            // check if erased type can be serialized
            final TypeMirror erasedType = getErasedType(type, processingEnvironment);
            String erasedClassName = erasedTypeString(erasedType, processingEnvironment);
            if (parameterizedSerializers.containsKey(erasedClassName))
            {
                return;
            }
        }
        else if (parameterizedSerializers.containsKey(qsn))
        {
            logger.error("Type " + qsn + " requires type paramter(s)");
            throw new UnableToCompleteException("Type " + qsn + " requires type paramter(s)");
        }

        if (qsn.startsWith("java.") || qsn.startsWith("javax."))
        {
            logger.error("Standard type " + qsn + " not supported in JSON encoding");
            throw new UnableToCompleteException(
                    "Standard type " + qsn + " not supported in JSON encoding: supported are: " + parameterizedSerializers + ", " + defaultSerializers);
        }

        if (isInterface(type))
        {
            logger.error("Interface " + qsn + " not supported in JSON encoding");
            throw new UnableToCompleteException("Interface " + qsn + " not supported in JSON encoding");
        }

        final TypeMirror ct = type;
        if (checkedType.contains(type))
        {
            return;
        }
        checkedType.add(ct);
        if (isAbstract(ct) && !allowAbstractType)
        {
            logger.error("Abstract type " + qsn + " not supported here");
            throw new UnableToCompleteException("Abstract type " + qsn + " not supported here");
        }
        final TypeElement element = (TypeElement) processingEnvironment.getTypeUtils().asElement(ct);
        if (element != null)
        {
            for (final VariableElement f : sortFields(element))
            {
                //    final TreeLogger branch = logger.branch(TreeLogger.DEBUG, "In type " + qsn + ", field " + f.getName());
                checkCanSerialize(logger, f.asType());
            }
        }
    }

    public String serializerFor(final TypeMirror t)
    {
        if (isArray(t))
        {
            final TypeMirror componentType = getArrayType(t);
            if (isPrimitive(componentType) || isBoxedPrimitive(componentType))
                return PrimitiveArraySerializer;
            else
                return ObjectArraySerializer + "<" + componentType.toString() + ">";
        }

        if (isStringMap(t))
        {
            return StringMapSerializer;
        }

        final String qsn = erasedTypeString(getErasedType(t, processingEnvironment), processingEnvironment);
        if (defaultSerializers.containsKey(qsn))
        {
            return defaultSerializers.get(qsn);
        }

        if (parameterizedSerializers.containsKey(qsn))
        {
            return parameterizedSerializers.get(qsn);
        }

        return generatedSerializers.get(qsn);
    }

    public static TypeElement getErasedType(Element typeElement, ProcessingEnvironment env)
    {
        final TypeMirror typeMirror = typeElement.asType();
        final TypeMirror erasedType = getErasedType(typeMirror, env);
        return (TypeElement) env.getTypeUtils().asElement(erasedType);
    }

    public static TypeMirror getErasedType(TypeMirror typeElement, ProcessingEnvironment env)
    {
        return env.getTypeUtils().erasure(typeElement);
    }

    private boolean isStringMap(final TypeMirror t)
    {
        if (!isParameterized(t))
        {
            return false;
        }
        TypeMirror erasedTyped = getErasedType(t, processingEnvironment);
        if (!isClass(erasedTyped) && !isInterface(erasedTyped))
        {
            return false;
        }

        List<? extends TypeMirror> typeParameters = ((DeclaredType) t).getTypeArguments();
        if (typeParameters.size() > 0 && typeParameters.get(0).toString().equals(String.class.getCanonicalName()))
        {
            TypeMirror t1 = erasedTyped;
            TypeMirror t2 = processingEnvironment.getElementUtils().getTypeElement(Map.class.getName()).asType();
            final boolean assignable = processingEnvironment.getTypeUtils().isAssignable(t1, t2);
            if (assignable)
            {
                return true;
            }

        }
        return false;
    }

    private void generateSingleton(final SourceWriter w, TypeElement targetType)
    {
        w.print("public static final ");
        final String serializerSimpleName = getSerializerSimpleName(targetType);
        w.print("javax.inject.Provider<" + serializerSimpleName + ">");

        w.print(" INSTANCE_PROVIDER = new javax.inject.Provider<");
        w.print(serializerSimpleName);
        w.println(">(){");
        w.indent();
        w.println("public " + serializerSimpleName + " get(){return INSTANCE;}");
        w.outdent();
        w.println("};");
        w.println();

        w.print("public static final ");
        w.print(serializerSimpleName);
        w.print(" INSTANCE = new ");
        w.print(serializerSimpleName);
        w.println("();");
        w.println();
    }

    private void generateInstanceMembers(final SourceWriter w, TypeElement targetType)
    {
        for (final VariableElement f : sortFields(targetType))
        {
            TypeMirror ft = f.asType();
            if (needsTypeParameter(ft, processingEnvironment))
            {
                final String serType = serializerFor(ft);
                w.print("private final ");
                w.print(serType);
                w.print(" ");
                w.print("ser_" + f.getSimpleName());
                w.print(" = ");
                boolean useProviders = true;
                generateSerializerReference(ft, w, useProviders);
                w.println(";");
            }
        }
        w.println();
    }

    void generateSerializerReference(final TypeMirror type, final SourceWriter w, boolean useProviders)
    {
        String serializerFor = serializerFor(type);
        if (isArray(type))
        {
            final TypeMirror componentType = getArrayType(type);
            if (isPrimitive(componentType) || isBoxedPrimitive(componentType))
            {
                w.print(PrimitiveArraySerializer);
                w.print(".INSTANCE");
            }
            else
            {
                w.print("new " + serializerFor + "(");
                generateSerializerReference(componentType, w, useProviders);
                w.print(")");
            }

        }
        else if (needsTypeParameter(type, processingEnvironment))
        {
            w.print("new " + serializerFor + "(");
            final List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
            int n = 0;
            if (isStringMap(type))
            {
                n++;
            }
            boolean first = true;
            for (; n < typeArgs.size(); n++)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    w.print(", ");
                }
                TypeMirror typeParameterElement = typeArgs.get(n);
                generateSerializerReference(typeParameterElement, w, useProviders);
            }
            w.print(")");
        }
        else
        {
            w.print(serializerFor + ".INSTANCE" + (useProviders ? "_PROVIDER" : ""));
        }
    }

    private void generateGetSets(final SourceWriter w, TypeElement targetType)
    {
        for (final VariableElement f : sortFields(targetType))
        {
            String fname = getFieldName(f);
            TypeMirror type = f.asType();
            if (isPrivate(f))
            {
                w.print("private static final native ");
                w.print(type.toString());
                w.print(" objectGet_" + fname);
                w.print("(");
                w.print(targetType.getQualifiedName().toString() + " instance");
                w.print(")");
                w.println("/*-{ ");
                w.print("  return instance.@");
                w.print(targetType.getQualifiedName().toString());
                w.print("::");
                w.print(fname);
                w.println(";");
                w.println("}-*/;");

                w.print("private static final native void ");
                w.print(" objectSet_" + fname);
                w.print("(");
                w.print(targetType.getQualifiedName().toString() + " instance, ");
                w.print(type.toString() + " value");
                w.print(")");
                w.println("/*-{ ");
                w.print("  instance.@");
                w.print(targetType.getQualifiedName().toString());
                w.print("::");
                w.print(fname);
                w.println(" = value;");

                w.println("}-*/;");
            }

            if (isPrimitiveChar(type) || isBoxedCharacter(type))
            {
                w.print("private static final native String");
                w.print(" jsonGet0_" + fname);
                w.print("(final Object instance)");
                w.println("/*-{ ");
                w.print("  return instance.");
                w.print(fname);
                w.println(";");
                w.println("}-*/;");

                w.print("private static final ");
                w.print(isPrimitiveChar(type) ? "char" : "Character");
                w.print(" jsonGet_" + fname);
                w.print("(Object instance)");
                w.println(" {");
                w.print("  return ");
                w.print(JsonSerializer);
                w.print(".toChar(");
                w.print("jsonGet0_" + fname);
                w.print("(instance)");
                w.println(");");
                w.println("}");
            }
            else
            {
                w.print("private static final native ");
                if (isArray(type))
                {
                    w.print("Object");
                }
                else if (isJsonPrimitive(type))
                {
                    w.print(type.toString());
                }
                else if (isBoxedPrimitive(type))
                {
                    w.print(boxedTypeToPrimitiveTypeName(type));
                }
                else
                {
                    w.print("Object");
                }
                w.print(" jsonGet_" + fname);
                w.print("(Object instance)");
                w.println("/*-{ ");

                w.print("  return instance.");
                w.print(fname);
                w.println(";");

                w.println("}-*/;");
            }

            w.println();
        }
    }

    private void generateEnumFromJson(final SourceWriter w, TypeElement targetType)
    {
        w.print("public ");
        w.print(targetType.getQualifiedName().toString());
        w.println(" fromJson(Object in) {");
        w.indent();
        w.print("  return in != null");
        w.print(" ? " + targetType.getQualifiedName().toString() + ".valueOf((String)in)");
        w.print(" : null");
        w.println(";");
        w.outdent();
        w.println("}");
        w.println();
    }

    private void generatePrintJson(final SourceWriter w, TypeElement targetType)
    {
        final List<VariableElement> fieldList = sortFields(targetType);
        w.print("protected int printJsonImpl(int fieldCount, StringBuilder sb, ");
        w.println("Object instance) {");
        w.indent();
        w.print("  final ");
        w.print(targetType.getQualifiedName().toString());
        w.print(" src = (");
        w.print(targetType.getQualifiedName().toString());
        w.println(")instance;");

        if (needsSuperSerializer(targetType))
        {
            w.print("fieldCount = super.printJsonImpl(fieldCount, sb, (");
            TypeElement superclass = getSuperclass(targetType);
            w.print(superclass.getQualifiedName().toString());
            w.println(")src);");
        }

        final String docomma = "if (fieldCount++ > 0) sb.append(\",\");";
        for (final VariableElement f : fieldList)
        {
            final String doget;
            TypeMirror ft = f.asType();
            String fname = getFieldName(f);
            if (isPrivate(f))
            {
                doget = "objectGet_" + fname + "(src)";
            }
            else
            {
                doget = "src." + fname;
            }

            final String doname = "sb.append(\"\\\"" + fname + "\\\":\");";
            if (isPrimitiveChar(ft) || isBoxedCharacter(ft))
            {
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(\"\\\"\");");
                w.println("sb.append(" + JsonSerializer_simple + ".escapeChar(" + doget + "));");
                w.println("sb.append(\"\\\"\");");
            }
            else if (isJsonString(ft))
            {
                w.println("if (" + doget + " != null) {");
                w.indent();
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(" + JsonUtils + ".escapeValue(" + doget + "));");
                w.outdent();
                w.println("}");
                w.println();
            }
            else if (isPrimitive(ft))
            {
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(" + doget + ");");
                w.println();
            }
            else if (isJsonPrimitive(ft) || isBoxedPrimitive(ft))
            {
                w.println("if (" + doget + " != null) {");
                w.indent();
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(" + doget + ");");
                w.println();
                w.outdent();
                w.println("}");
            }
            else
            {
                w.println("if (" + doget + " != null) {");
                w.indent();
                w.println(docomma);
                w.println(doname);
                if (needsTypeParameter(ft, processingEnvironment))
                {
                    w.print("ser_" + fname);
                }
                else
                {
                    w.print(serializerFor(ft) + ".INSTANCE");
                }
                w.println(".printJson(sb, " + doget + ");");
                w.outdent();
                w.println("}");
                w.println();
            }
        }

        w.println("return fieldCount;");
        w.outdent();
        w.println("}");
        w.println();
    }

    public static PackageElement getPackage(Element type)
    {
        while (type.getKind() != ElementKind.PACKAGE)
        {
            type = type.getEnclosingElement();
        }
        return (PackageElement) type;
    }

    private void generateFromJson(final SourceWriter w, TypeElement targetType)
    {
        w.print("public ");
        w.print(targetType.getQualifiedName().toString());
        w.println(" fromJson(Object in) {");
        w.indent();
        if (isAbstract(targetType.asType()))
        {
            w.println("throw new UnsupportedOperationException();");
        }
        else
        {
            w.println("if (in == null) return null;");
            w.print("final ");
            w.print(targetType.getQualifiedName().toString());
            w.print(" dst = new ");
            w.println(targetType.getQualifiedName().toString() + "();");
            w.println("fromJsonImpl(in, dst);");
            w.println("return dst;");
        }
        w.outdent();
        w.println("}");
        w.println();

        w.print("protected void fromJsonImpl(Object jso,");
        w.print(targetType.getQualifiedName().toString());
        w.println(" dst) {");
        w.indent();
        if (needsSuperSerializer(targetType))
        {
            w.print("super.fromJsonImpl(jso, (");
            w.print(targetType.getSuperclass().toString());
            w.println(")dst);");
        }

        for (final VariableElement f : sortFields(targetType))
        {
            String fname = getFieldName(f);
            final String doget = "jsonGet_" + fname + "(jso)";
            final String doset0, doset1;

            if (isPrivate(f))
            {
                doset0 = "objectSet_" + fname + "(dst, ";
                doset1 = ")";
            }
            else
            {
                doset0 = "dst." + fname + " = ";
                doset1 = "";
            }

            TypeMirror type = f.asType();
            if (isArray(type))
            {
                final TypeMirror ct = getArrayType(type);
                w.println("if (" + doget + " != null) {");
                w.indent();
                w.print("final ");
                w.print(ct.toString());
                w.print("[] tmp = new ");
                w.print(ct.toString());
                w.print("[");
                w.print(ObjectArraySerializer);
                w.print(".size(" + doget + ")");
                w.println("];");

                w.println("ser_" + fname + ".fromJson(" + doget + ", tmp);");

                w.print(doset0);
                w.print("tmp");
                w.print(doset1);
                w.println(";");
                w.outdent();
                w.println("}");

            }
            else if (isJsonPrimitive(type))
            {
                w.print(doset0);
                w.print(doget);
                w.print(doset1);
                w.println(";");

            }
            else if (isBoxedPrimitive(type))
            {
                w.print(doset0);
                w.print("( " + doget + " != null) ? ");
                //w.print("new " + type.getQualifiedSourceName() + "(");
                w.print(doget);
                //w.print(")");
                w.print(":null");
                w.print(doset1);
                w.println(";");

            }
            else
            {
                w.print(doset0);
                if (needsTypeParameter(type, processingEnvironment))
                {
                    w.print("ser_" + fname);
                }
                else
                {
                    String serializerFor = serializerFor(type);
                    w.print(serializerFor + ".INSTANCE");
                }
                w.print(".fromJson(" + doget + ")");
                w.print(doset1);
                w.println(";");
            }
        }
        w.outdent();
        w.println("}");
        w.println();
    }

    public static boolean isArray(TypeMirror typeMirror)
    {
        boolean b = typeMirror.getKind() == TypeKind.ARRAY;
        return b;
    }

    public boolean isAbstract(final TypeMirror t)
    {
        final Element element = processingEnvironment.getTypeUtils().asElement(t);
        return element != null && element.getModifiers().contains(Modifier.ABSTRACT);
    }

    public boolean isPrivate(final Element element)
    {
        return element != null && element.getModifiers().contains(Modifier.PRIVATE);
    }

    public boolean isInterface(final TypeMirror t)
    {
        final Element element = processingEnvironment.getTypeUtils().asElement(t);
        boolean b = element != null && element.getKind() == ElementKind.INTERFACE;
        return b;
    }

    public boolean isClass(final TypeMirror t)
    {
        final Element element = processingEnvironment.getTypeUtils().asElement(t);
        boolean b = element != null && element.getKind() == ElementKind.CLASS;
        return b;
    }

    public boolean isEnum(final TypeMirror t)
    {
        final Element element = processingEnvironment.getTypeUtils().asElement(t);
        return element != null && element.getKind() == ElementKind.ENUM;
    }
    static public boolean isVoid(final TypeMirror t)
    {
        return  t.toString().equals("java.lang.Void");
    }

    static boolean isJsonPrimitive(final TypeElement t)
    {
        return isPrimitive(t) || isJsonString(t);
    }

    static boolean isJsonPrimitive(final TypeMirror t)
    {
        return isPrimitive(t) || isJsonString(t);
    }

    private TypeElement getSuperclass(TypeElement targetType)
    {
        TypeMirror superclass = targetType.getSuperclass();
        final TypeElement ele = (TypeElement) processingEnvironment.getTypeUtils().asElement(superclass);
        return ele;
    }

    private TypeMirror getArrayType(TypeMirror type)
    {
        final ArrayType arrayType = (ArrayType) type;
        final TypeMirror componentType = arrayType.getComponentType();
        if (componentType instanceof ArrayType)
        {
            return getArrayType(componentType);
        }
        return componentType;
    }

    private String getFieldName(VariableElement f)
    {
        return f.getSimpleName().toString();
    }

    // return paramter type
    public static boolean isParameterized(final TypeMirror t)
    {
        if (!(t instanceof DeclaredType))
        {
            return false;
        }
        List typeParameters = ((DeclaredType) t).getTypeArguments();
        return typeParameters.size() > 0;
    }

    static boolean isPrimitiveLong(final TypeMirror typeMirror)
    {
        if (typeMirror instanceof PrimitiveType)
        {
            boolean b = ((PrimitiveType) typeMirror).getKind() == TypeKind.LONG;
            return b;
        }
        else
        {
            return false;
        }
    }

    private static boolean isPrimitiveChar(TypeMirror typeMirror)
    {
        if (typeMirror instanceof PrimitiveType)
        {
            boolean b = ((PrimitiveType) typeMirror).getKind() == TypeKind.CHAR;
            return b;
        }
        else
        {
            return false;
        }
    }

    static boolean isPrimitive(final Element t)
    {
        TypeMirror typeMirror = t.asType();
        return isPrimitive(typeMirror);
    }

    public static boolean isPrimitive(TypeMirror typeMirror)
    {
        if (typeMirror instanceof PrimitiveType)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    static boolean isBoxedPrimitive(TypeMirror typeMirror)
    {
        final String qsn = typeMirror.toString();
        return qsn.equals(Boolean.class.getCanonicalName()) || qsn.equals(Byte.class.getCanonicalName()) || qsn.equals(Character.class.getCanonicalName())
                || qsn.equals(Double.class.getCanonicalName()) || qsn.equals(Float.class.getCanonicalName()) || qsn.equals(Integer.class.getCanonicalName())
                || qsn.equals(Short.class.getCanonicalName());
    }

    static boolean isBoxedCharacter(TypeElement t)
    {
        return t.getQualifiedName().toString().equals(Character.class.getCanonicalName());
    }

    static boolean isBoxedCharacter(TypeMirror t)
    {
        return t.toString().equals(Character.class.getCanonicalName());
    }

    private String boxedTypeToPrimitiveTypeName(TypeMirror t)
    {
        final String qsn = t.toString();
        if (qsn.equals(Boolean.class.getCanonicalName()))
            return "Boolean";
        if (qsn.equals(Byte.class.getCanonicalName()))
            return "Byte";
        if (qsn.equals(Character.class.getCanonicalName()))
            return "java.lang.String";
        if (qsn.equals(Double.class.getCanonicalName()))
            return "Double";
        if (qsn.equals(Float.class.getCanonicalName()))
            return "Float";
        if (qsn.equals(Integer.class.getCanonicalName()))
            return "Integer";
        if (qsn.equals(Short.class.getCanonicalName()))
            return "Short";
        throw new IllegalArgumentException(t + " is not a boxed type");
    }

    static boolean isJsonString(final TypeElement t)
    {
        return t.getQualifiedName().toString().equals(String.class.getCanonicalName());
    }

    static boolean isJsonString(final TypeMirror t)
    {
        return t.toString().equals(String.class.getCanonicalName());
    }

    private SourceWriter getSourceWriter(TypeElement targetType) 
    {
        String serializerSimpleName = getSerializerSimpleName(targetType);
        final String pkgName = getPackage(targetType).getQualifiedName().toString();
        final SourceWriter pw = new SourceWriter(pkgName,serializerSimpleName, processingEnvironment);

        pw.println("package " + pkgName + ";");
        pw.println();
        addImport(pw, JsonSerializer);
        String superclass;
        if (isEnum(targetType.asType()))
        {
            addImport(pw, EnumSerializer);

            superclass = EnumSerializer_simple + "<" + targetType.getQualifiedName().toString() + ">";

        }
        else if (needsSuperSerializer(targetType))
        {
            superclass = getSerializerQualifiedName(getSuperclass(targetType));
        }
        else
        {
            addImport(pw, ObjectSerializer);
            superclass = ObjectSerializer_simple + "<" + targetType.getQualifiedName().toString() + ">";
        }
        pw.println();
        pw.println(getGeneratorString());
        pw.println("public class " + serializerSimpleName + " extends " + superclass);
        pw.println("{");
        pw.indent();
        return pw;
    }

   
    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    private void addImport(SourceWriter pw, String canonicalName)
    {
        pw.println("import " + canonicalName + ";");
    }

    private boolean needsSuperSerializer(TypeElement type)
    {
        type = getSuperclass(type);
        while (type != null && !Object.class.getName().equals(type.getQualifiedName().toString()))
        {
            if (sortFields(type).size() > 0)
            {
                return true;
            }
            type = getSuperclass(type);
        }
        return false;
    }

    private String getSerializerQualifiedName(final TypeElement targetType)
    {
        final String[] name;
        name = GwtProxyCreator.synthesizeTopLevelClassName(targetType, SER_SUFFIX, processingEnvironment);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    private String getSerializerSimpleName(TypeElement targetType)
    {

        return GwtProxyCreator.synthesizeTopLevelClassName(targetType, SER_SUFFIX, processingEnvironment)[1];
    }

    boolean needsTypeParameter(final TypeMirror ft, ProcessingEnvironment processingEnvironment)
    {
        final TypeMirror erasedType = getErasedType(ft, processingEnvironment);
        final String key = erasedTypeString(erasedType, processingEnvironment);
        return isArray(ft) || (isParameterized(ft) && parameterizedSerializers.containsKey(key));
    }

    private static List<VariableElement> sortFields(final TypeElement targetType)
    {
        final ArrayList<VariableElement> r = new ArrayList<VariableElement>();
        for (final Element f : targetType.getEnclosedElements())
        {
            ElementKind kind = f.getKind();
            if (kind != ElementKind.FIELD)
            {
                continue;
            }
            Set<Modifier> modifiers = f.getModifiers();

            if (!modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.TRANSIENT) && !modifiers.contains(Modifier.FINAL))
            {
                r.add((VariableElement) f);
            }
        }
        Collections.sort(r, FIELD_COMP);
        return r;
    }

    public static String erasedTypeString(final TypeMirror type, ProcessingEnvironment env)
    {
        Element element = env.getTypeUtils().asElement(type);
        if (element instanceof TypeElement)
        {
            return erasedTypeString((TypeElement) element);
        }
        else
        {
            final int indexOf = type.toString().indexOf("<");
            if (type instanceof DeclaredType && indexOf > 0)
            {
                return type.toString().substring(0, indexOf);
            }
            else
            {
                return type.toString();
            }
        }
    }

    public static String erasedTypeString(final TypeElement erasedType)
    {
        final String string = erasedType.getQualifiedName().toString();
        return string;
    }

}
