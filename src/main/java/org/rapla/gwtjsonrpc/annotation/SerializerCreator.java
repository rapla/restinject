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

package org.rapla.gwtjsonrpc.annotation;

import com.google.gwt.core.client.JavaScriptObject;

import javax.lang.model.element.*;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class SerializerCreator implements SerializerClasses
{
    private static final String SER_SUFFIX = "_JsonSerializer";
    private static final Comparator<Element> FIELD_COMP = new Comparator<Element>()
    {
        @Override public int compare(final Element o1, final Element o2)
        {
            return o1.getSimpleName().toString().compareTo(o2.getSimpleName().toString());
        }
    };

    private static final HashMap<String, String> defaultSerializers;
    private static final HashMap<String, String> parameterizedSerializers;

    static
    {
        defaultSerializers = new HashMap<String, String>();
        parameterizedSerializers = new HashMap<String, String>();

        defaultSerializers.put(String.class.getCanonicalName(), JavaLangString_JsonSerializer);
        defaultSerializers.put(Date.class.getCanonicalName(), JavaUtilDate_JsonSerializer);
        //    defaultSerializers.put(java.sql.Date.class.getCanonicalName(),
        //        JavaSqlDate_JsonSerializer.class.getCanonicalName());
        //    defaultSerializers.put(java.sql.Timestamp.class.getCanonicalName(),
        //        JavaSqlTimestamp_JsonSerializer.class.getCanonicalName());
        parameterizedSerializers.put(List.class.getCanonicalName(), ListSerializer);
        parameterizedSerializers.put(Map.class.getCanonicalName(), ObjectMapSerializer);
        parameterizedSerializers.put(Set.class.getCanonicalName(), SetSerializer);
    }

    private final HashMap<String, String> generatedSerializers;
    private TypeElement targetType;

    SerializerCreator()
    {
        generatedSerializers = new HashMap<String, String>();
    }

    String create(final TypeElement targetType, final TreeLogger logger) throws UnableToCompleteException, IOException
    {
        if (isParameterized(targetType) || isArray(targetType))
        {
            ensureSerializersForTypeParameters(logger, targetType);
        }
        String sClassName = serializerFor(targetType);
        if (sClassName != null)
        {
            return sClassName;
        }

        checkCanSerialize(logger, targetType, true);
        recursivelyCreateSerializers(logger, targetType);

        this.targetType = targetType;
        final PrintWriter srcWriter = getSourceWriter(logger);
        final String sn = getSerializerQualifiedName(targetType);
        if (!generatedSerializers.containsKey(targetType.getQualifiedName().toString()))
        {
            generatedSerializers.put(targetType.getQualifiedName().toString(), sn);
        }
        if (srcWriter == null)
        {
            return sn;
        }


        if (!isAbstract(targetType))
        {
            generateSingleton(srcWriter);
        }
        if (isEnum(targetType))
        {
            generateEnumFromJson(srcWriter);
        }
        else
        {
            generateInstanceMembers(srcWriter);
            generatePrintJson(srcWriter);
            generateFromJson(srcWriter);
            generateGetSets(srcWriter);
        }
        return sn;
    }

    private void recursivelyCreateSerializers(final TreeLogger logger, final TypeElement targetType) throws UnableToCompleteException, IOException
    {
        if (isPrimitive(targetType) || isBoxedPrimitive(targetType))
        {
            return;
        }

        final TypeElement targetClass = targetType;
        if (needsSuperSerializer(targetClass))
        {
            create(getSuperclass(targetClass), logger);
        }

        for (final TypeElement f : sortFields(targetClass))
        {
            ensureSerializer(logger, f.getType());
        }
    }

    Set<TypeElement> createdType = new HashSet<TypeElement>();

    private void ensureSerializer(final TreeLogger logger, final TypeElement type) throws UnableToCompleteException, IOException
    {
        if (ensureSerializersForTypeParameters(logger, type))
        {
            return;
        }

        final String qsn = type.getQualifiedName().toString();
        if (defaultSerializers.containsKey(qsn) || parameterizedSerializers.containsKey(qsn))
        {
            return;
        }

        TypeElement type2 =  type;
        if (createdType.contains(type2))
        {
            return;
        }
        createdType.add(type2);
        create(type2, logger);
    }

    private boolean ensureSerializersForTypeParameters(final TreeLogger logger, final TypeElement type) throws UnableToCompleteException, IOException
    {
        if (isJsonPrimitive(type) || isBoxedPrimitive(type))
        {
            return true;
        }

        if (isArray(type) )
        {
            ensureSerializer(logger, type.isArray().getComponentType());
            return true;
        }

        if (isParameterized(type) )
        {
            for (final TypeParameterElement t : type.getTypeParameters())
            {
                ensureSerializer(logger, (TypeElement)t);
            }
        }

        return false;
    }

    void checkCanSerialize(final TreeLogger logger, final Element type) throws UnableToCompleteException
    {
        checkCanSerialize(logger, type, false);
    }

    Set<TypeElement> checkedType = new HashSet<TypeElement>();

    void checkCanSerialize(final TreeLogger logger, final TypeElement type, boolean allowAbstractType) throws UnableToCompleteException
    {
        if (isPrimitiveLong(type))
        {
            logger.error("Type 'long' not supported in JSON encoding");
            throw new UnableToCompleteException();
        }

        //    if (type.isPrimitive() == JPrimitiveType.VOID) {
        //      logger.log(TreeLogger.ERROR,
        //          "Type 'void' not supported in JSON encoding", null);
        //      throw new UnableToCompleteException();
        //    }

        final String qsn = type.getQualifiedName().toString();
        if (isEnum(type))
        {
            return;
        }

        if (isJsonPrimitive(type) || isBoxedPrimitive(type))
        {
            return;
        }

        if (isArray(type) )
        {
            final TypeElement leafType = type.isArray().getLeafType();
            if (isPrimitive(leafType) || isBoxedPrimitive(leafType))
            {
                if (type.isArray().getRank() != 1)
                {
                    logger.error("gwtjsonrpc does not support " + "(de)serializing of multi-dimensional arrays of primitves");
                    // To work around this, we would need to generate serializers for
                    // them, this can be considered a todo
                    throw new UnableToCompleteException();
                }
                else
                    // Rank 1 arrays work fine.
                    return;
            }
            checkCanSerialize(logger, type.isArray().getComponentType());
            return;
        }

        if (defaultSerializers.containsKey(qsn))
        {
            return;
        }

        if (isParameterized(type))
        {
            List<? extends TypeParameterElement> typeArgs = type.getTypeParameters();
            for (final TypeParameterElement t : typeArgs)
            {
                checkCanSerialize(logger, t);
            }
            if (parameterizedSerializers.containsKey(qsn))
            {
                return;
            }
        }
        else if (parameterizedSerializers.containsKey(qsn))
        {
            logger.error("Type " + qsn + " requires type paramter(s)");
            throw new UnableToCompleteException();
        }

        if (qsn.startsWith("java.") || qsn.startsWith("javax."))
        {
            logger.error("Standard type " + qsn + " not supported in JSON encoding");
            throw new UnableToCompleteException();
        }

        if (isInterface(type))
        {
            logger.error("Interface " + qsn + " not supported in JSON encoding");
            throw new UnableToCompleteException();
        }

        final TypeElement ct = (TypeElement) type;
        if (checkedType.contains(ct))
        {
            return;
        }
        checkedType.add(ct);
        if (isAbstract(ct) && !allowAbstractType)
        {
            logger.error("Abstract type " + qsn + " not supported here");
            throw new UnableToCompleteException();
        }
        for (final VariableElement f : sortFields(ct))
        {
            TypeMirror typeMirror = f.asType();
            //    final TreeLogger branch = logger.branch(TreeLogger.DEBUG, "In type " + qsn + ", field " + f.getName());
            checkCanSerialize(logger, f.getType());
        }
    }

    String serializerFor(final TypeElement t)
    {
        if (isArray(t) )
        {
            final TypeElement componentType = t.isArray().getComponentType();
            if (isPrimitive(componentType) || isBoxedPrimitive(componentType))
                return PrimitiveArraySerializer;
            else
                return ObjectArraySerializer + "<" + componentType.getQualifiedName().toString() + ">";
        }

        if (isStringMap(t))
        {
            return StringMapSerializer;
        }

        final String qsn = t.getQualifiedName().toString();
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

    private boolean isStringMap(final TypeElement t)
    {
        if (!isParameterized(t))
        {
            return false;
        }
        return  t.getErasedType().isClassOrInterface() != null && t.isParameterized().getTypeArgs().length > 0 && t
                .isParameterized().getTypeArgs()[0].getQualifiedSourceName().equals(String.class.getName()) && t.getErasedType().isClassOrInterface()
                .isAssignableTo(context.getTypeOracle().findType(Map.class.getName()));
    }

    private void generateSingleton(final PrintWriter w)
    {
        w.print("public static final ");
        w.print("javax.inject.Provider<" + getSerializerSimpleName() + ">");

        w.print(" INSTANCE_PROVIDER = new javax.inject.Provider<");
        w.print(getSerializerSimpleName());
        w.println(">(){");
        w.print("public " + getSerializerSimpleName() + " get(){return INSTANCE;} ");
        w.println("};");
        w.println();

        w.print("public static final ");
        w.print(getSerializerSimpleName());
        w.print(" INSTANCE = new ");
        w.print(getSerializerSimpleName());
        w.println("();");
        w.println();
    }

    private void generateInstanceMembers(final PrintWriter w)
    {
        for (final TypeElement f : sortFields(targetType))
        {
            TypeMirror ft = f.asType();
            if (needsTypeParameter(ft))
            {
                final String serType = serializerFor(ft);
                w.print("private final ");
                w.print(serType);
                w.print(" ");
                w.print("ser_" + f.getName());
                w.print(" = ");
                boolean useProviders = true;
                generateSerializerReference(ft, w, useProviders);
                w.println(";");
            }
        }
        w.println();
    }

    void generateSerializerReference(final TypeElement type, final PrintWriter w, boolean useProviders)
    {
        Collections.sort(Collections.emptyList(), (o1, o2) -> o1.hashCode() - o2.hashCode());
        Collections.emptyList().stream().filter((o) -> o.hashCode() > 1);
        String serializerFor = serializerFor(type);
        if (isArray(type))
        {
            final TypeElement componentType = type.isArray().getComponentType();
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
        else if (needsTypeParameter(type))
        {
            w.print("new " + serializerFor + "(");
            final List<? extends TypeParameterElement> typeArgs = type.getTypeParameters();
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
                generateSerializerReference(typeArgs.get(n), w, useProviders);
            }
            w.print(")");

        }
        else
        {
            //      String sourceName = type.getQualifiedSourceName();

            w.print(serializerFor + ".INSTANCE" + (useProviders ? "_PROVIDER" : ""));
        }
    }

    private void generateGetSets(final PrintWriter w)
    {
        for (final VariableElement f : sortFields(targetType))
        {
            String fname = f.getName();
            TypeElement type= f.getType;
            if (isPrivate(f))
            {
                w.print("private static final native ");
                w.print(type.getQualifiedName().toString());
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
                w.print(type.getQualifiedName().toString() + " value");
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
                w.print("(final JavaScriptObject instance)");
                w.println("/*-{ ");
                w.print("  return instance.");
                w.print(fname);
                w.println(";");
                w.println("}-*/;");

                w.print("private static final ");
                w.print(isPrimitiveChar(type)  ? "char" : "Character");
                w.print(" jsonGet_" + fname);
                w.print("(JavaScriptObject instance)");
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
                if (isArray(type) )
                {
                    w.print("JavaScriptObject");
                }
                else if (isJsonPrimitive(type))
                {
                    w.print(type.getQualifiedName().toString());
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
                w.print("(JavaScriptObject instance)");
                w.println("/*-{ ");

                w.print("  return instance.");
                w.print(fname);
                w.println(";");

                w.println("}-*/;");
            }

            w.println();
        }
    }

    private void generateEnumFromJson(final PrintWriter w)
    {
        w.print("public ");
        w.print(targetType.getQualifiedName().toString());
        w.println(" fromJson(Object in) {");
        w.print("  return in != null");
        w.print(" ? " + targetType.getQualifiedName().toString() + ".valueOf((String)in)");
        w.print(" : null");
        w.println(";");
        w.println("}");
        w.println();
    }

    private void generatePrintJson(final PrintWriter w)
    {
        final List<Element> fieldList = sortFields(targetType);
        w.print("protected int printJsonImpl(int fieldCount, StringBuilder sb, ");
        w.println("Object instance) {");

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
        for (final TypeElement f : fieldList)
        {
            final String doget;
            TypeElement ft = f.getType();
            String fname = f.getName();
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
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(" + JsonSerializer_simple + ".escapeString(" + doget + "));");
                w.println("}");
                w.println();
            }
            else if (isPrimitive(ft) )
            {
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(" + doget + ");");
                w.println();
            }
            else if (isJsonPrimitive(ft) || isBoxedPrimitive(ft))
            {
                w.println("if (" + doget + " != null) {");
                w.println(docomma);
                w.println(doname);
                w.println("sb.append(" + doget + ");");
                w.println();
                w.println("}");
            }
            else
            {
                w.println("if (" + doget + " != null) {");
                w.println(docomma);
                w.println(doname);
                if (needsTypeParameter(ft))
                {
                    w.print("ser_" + fname);
                }
                else
                {
                    w.print(serializerFor(ft) + ".INSTANCE");
                }
                w.println(".printJson(sb, " + doget + ");");
                w.println("}");
                w.println();
            }
        }

        w.println("return fieldCount;");
        w.println("}");
        w.println();
    }

    static private TypeElement getSuperclass(TypeElement targetType)
    {
    }

    public static PackageElement getPackage(Element type) {
        while (type.getKind() != ElementKind.PACKAGE) {
            type = type.getEnclosingElement();
        }
        return (PackageElement) type;
    }


    private void generateFromJson(final PrintWriter w)
    {
        w.print("public ");
        w.print(targetType.getQualifiedName().toString());
        w.println(" fromJson(Object in) {");
        if (isAbstract(targetType))
        {
            w.println("throw new UnsupportedOperationException();");
        }
        else
        {
            w.println("if (in == null) return null;");
            w.println("final JavaScriptObject jso = (JavaScriptObject)in;");
            w.print("final ");
            w.print(targetType.getQualifiedName().toString());
            w.print(" dst = new ");
            w.println(targetType.getQualifiedName().toString() + "();");
            w.println("fromJsonImpl(jso, dst);");
            w.println("return dst;");
        }
        w.println("}");
        w.println();

        w.print("protected void fromJsonImpl(JavaScriptObject jso,");
        w.print(targetType.getQualifiedName().toString());
        w.println(" dst) {");

        if (needsSuperSerializer(targetType))
        {
            w.print("super.fromJsonImpl(jso, (");
            w.print(getSuperclass(targetType).getQualifiedName().toString());
            w.println(")dst);");
        }

        for (final TypeElement f : sortFields(targetType))
        {
            String fname = f.getName();
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

            TypeElement type = f.getType();
            if (isArray(type) )
            {
                final TypeElement ct = isArray(type).getComponentType();
                w.println("if (" + doget + " != null) {");

                w.print("final ");
                w.print(ct.getQualifiedName().toString());
                w.print("[] tmp = new ");
                w.print(ct.getQualifiedName().toString());
                w.print("[");
                w.print(ObjectArraySerializer);
                w.print(".size(" + doget + ")");
                w.println("];");

                w.println("ser_" + fname + ".fromJson(" + doget + ", tmp);");

                w.print(doset0);
                w.print("tmp");
                w.print(doset1);
                w.println(";");

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
                if (needsTypeParameter(type))
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

        w.println("}");
        w.println();
    }

    public static boolean isArray(final Element t)
    {
        TypeMirror typeMirror = t.asType();
        boolean b = typeMirror.getKind() == TypeKind.ARRAY;
        return b;
    }

    public static boolean isAbstract(final Element t)
    {
        return t.getModifiers().contains(Modifier.ABSTRACT);
    }

    public static boolean isPrivate(final Element t)
    {
        return t.getModifiers().contains(Modifier.PRIVATE);
    }

    public static boolean isInterface(final Element t)
    {
        boolean b = t.getKind() == ElementKind.INTERFACE;
        return b;
    }

    public static boolean isEnum(final Element t)
    {
        return t.getKind() == ElementKind.ENUM;
    }


    static boolean isJsonPrimitive(final Element t)
    {
    }


    // return paramter type
    public static boolean isParameterized(final TypeElement t)
    {
        List typeParameters = t.getTypeParameters();
        return typeParameters.size() > 0;
    }

    static boolean isPrimitiveLong(final Element t)
    {
        TypeMirror typeMirror = t.asType();
        if ( typeMirror instanceof  PrimitiveType)
        {
            boolean b = ((PrimitiveType) typeMirror).getKind() == TypeKind.LONG;
            return b;
        }
        else
        {
            return false;
        }
    }

    static boolean isPrimitiveChar(final Element t)
    {
        TypeMirror typeMirror = t.asType();
        if ( typeMirror instanceof  PrimitiveType)
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
        if ( typeMirror instanceof  PrimitiveType)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    static boolean isBoxedPrimitive(final TypeElement t)
    {
        final String qsn = t.getQualifiedName().toString();
        return qsn.equals(Boolean.class.getCanonicalName()) || qsn.equals(Byte.class.getCanonicalName()) || isBoxedCharacter(t) || qsn
                .equals(Double.class.getCanonicalName()) || qsn.equals(Float.class.getCanonicalName()) || qsn.equals(Integer.class.getCanonicalName()) || qsn
                .equals(Short.class.getCanonicalName());
    }

    static boolean isBoxedCharacter(TypeElement t)
    {
        return t.getQualifiedName().toString().equals(Character.class.getCanonicalName());
    }

    private String boxedTypeToPrimitiveTypeName(TypeElement t)
    {
        final String qsn = t.getQualifiedName().toString();
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


    private PrintWriter getSourceWriter(final TreeLogger logger) throws IOException
    {
        String serializerSimpleName = getSerializerSimpleName();
        final String pkgName = getSerializerPackageName();
        JavaFileObject sourceFile = logger.getFiler().createSourceFile(pkgName + "." + serializerSimpleName);
        final PrintWriter pw = new PrintWriter(sourceFile.openWriter());

        pw.println( "package " + pkgName + ";");
        addImport(pw, JavaScriptObject.class.getCanonicalName());
        addImport(pw, JsonSerializer);
        String superclass;
        if (isEnum(targetType))
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
            addImport(pw,ObjectSerializer);
            superclass = ObjectSerializer_simple + "<" + targetType.getQualifiedName().toString() + ">";
        }
        pw.println("class " + serializerSimpleName + " extends " + superclass);
        return pw;
    }

    private void addImport(PrintWriter pw, String canonicalName)
    {
        pw.println("import "+ canonicalName + ";");
    }

    private static boolean needsSuperSerializer(TypeElement type)
    {
        type = getSuperclass(type);
        while (!Object.class.getName().equals(type.getQualifiedName().toString()))
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
        name = org.rapla.gwtjsonrpc.annotation.ProxyCreator.synthesizeTopLevelClassName(targetType, SER_SUFFIX);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    private String getSerializerSimpleName()
    {
        return org.rapla.gwtjsonrpc.annotation.ProxyCreator.synthesizeTopLevelClassName(targetType, SER_SUFFIX)[1];
    }

    static boolean needsTypeParameter(final TypeElement ft)
    {
        return isArray(ft) || (isParameterized(ft)  && parameterizedSerializers.containsKey(ft.getQualifiedName().toString()
        ));
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

            if (!modifiers.contains( Modifier.STATIC) && !modifiers.contains(Modifier.TRANSIENT) && !modifiers.contains(Modifier.FINAL))
            {
                r.add((VariableElement)f);
            }
        }
        Collections.sort(r, FIELD_COMP);
        return r;
    }
}