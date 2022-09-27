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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class SerializeCheck
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

    private final HashSet<String> defaultSerializers;
    private final HashSet<String> parameterizedSerializers;
    private final String generatorName;

    public SerializeCheck(ProcessingEnvironment processingEnvironment, String generatorName)
    {
        this.processingEnvironment = processingEnvironment;
        this.generatorName = generatorName;
        defaultSerializers = new HashSet<>();
        parameterizedSerializers = new HashSet<>();

        defaultSerializers.add(String.class.getCanonicalName());
        defaultSerializers.add(Integer.class.getCanonicalName());
        defaultSerializers.add(Date.class.getCanonicalName());
        parameterizedSerializers.add(List.class.getCanonicalName());
        parameterizedSerializers.add(Map.class.getCanonicalName());
        parameterizedSerializers.add(Set.class.getCanonicalName());
        parameterizedSerializers.add(SortedSet.class.getCanonicalName());
        parameterizedSerializers.add(Collection.class.getCanonicalName());
    }

    void checkCanSerialize(final TypeMirror type) throws UnableToCompleteException
    {
        checkCanSerialize(type, false);
    }

    Set<TypeMirror> checkedType = new HashSet<>();

    void checkCanSerialize(final TypeMirror type, boolean allowAbstractType) throws UnableToCompleteException
    {
        if (isPrimitiveLong(type))
        {
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
                if (AbstractClientProxyCreator.getRank(arrayType) != 1)
                {
                    // To work around this, we would need to generate serializers for
                    // them, this can be considered a todo
                    throw new UnableToCompleteException("restinject does not support " + "(de)serializing of multi-dimensional arrays of primitves");
                }
                else
                    // Rank 1 arrays work fine.
                    return;
            }
            checkCanSerialize(getArrayType(type));
            return;
        }

        final String qsn = type.toString();
        if (defaultSerializers.contains(qsn))
        {
            return;
        }
        if (isParameterized(type))
        {
            List<? extends TypeMirror> typeArgs = ((DeclaredType) type).getTypeArguments();
            // check if parameter types can be serialized
            for (final TypeMirror t : typeArgs)
            {
                checkCanSerialize(t);
            }
            // check if erased type can be serialized
            final TypeMirror erasedType = getErasedType(type, processingEnvironment);
            String erasedClassName = erasedTypeString(erasedType, processingEnvironment);
            if (parameterizedSerializers.contains(erasedClassName))
            {
                return;
            }
        }
        else if (parameterizedSerializers.contains(qsn))
        {
            throw new UnableToCompleteException("Type " + qsn + " requires type paramter(s)");
        }

        if (qsn.startsWith("java.") || qsn.startsWith("javax."))
        {
            throw new UnableToCompleteException(
                    "Standard type " + qsn + " not supported in JSON encoding: supported are: " + parameterizedSerializers + ", " + defaultSerializers);
        }

        if (isInterface(type))
        {
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
            throw new UnableToCompleteException("Abstract type " + qsn + " not supported here");
        }
        final TypeElement element = (TypeElement) processingEnvironment.getTypeUtils().asElement(ct);
        if (element != null)
        {
            for (final VariableElement f : sortFields(element))
            {
                //    final TreeLogger branch = logger.branch(TreeLogger.DEBUG, "In type " + qsn + ", field " + f.getName());
                checkCanSerialize(f.asType());
            }
        }
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


    static boolean isJsonString(final TypeElement t)
    {
        return t.getQualifiedName().toString().equals(String.class.getCanonicalName());
    }

    static boolean isJsonString(final TypeMirror t)
    {
        return t.toString().equals(String.class.getCanonicalName());
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
