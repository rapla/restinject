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

package org.rapla.gwtjsonrpc.rebind;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.dev.generator.NameFactory;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.rapla.gwtjsonrpc.RemoteJsonMethod;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

class ProxyCreator implements SerializerClasses
{
    private static final String PROXY_SUFFIX = "_JsonProxy";
    private JClassType svcInf;
    String futureResultClassName;
    private SerializerCreator serializerCreator;
    private ResultDeserializerCreator deserializerCreator;
    private int instanceField;

    ProxyCreator(final JClassType remoteService)
    {
        svcInf = remoteService;
    }

    String create(final TreeLogger logger, final GeneratorContext context) throws UnableToCompleteException
    {
        serializerCreator = new SerializerCreator(context);
        deserializerCreator = new ResultDeserializerCreator(context, serializerCreator);
        futureResultClassName = FutureResultImpl;
        checkMethods(logger, context);

        final SourceWriter srcWriter = getSourceWriter(logger, context);
        if (srcWriter == null)
        {
            return getProxyQualifiedName();
        }

        generateProxyConstructor(logger, srcWriter);
        generateProxyCallCreator(logger, srcWriter);
        generateProxyMethods(logger, srcWriter);
        srcWriter.commit(logger);

        return getProxyQualifiedName();
    }

    private void checkMethods(final TreeLogger logger, @SuppressWarnings("unused") final GeneratorContext context) throws UnableToCompleteException
    {
        final Set<String> declaredNames = new HashSet<String>();
        final JMethod[] methodList = svcInf.getOverridableMethods();
        for (final JMethod m : methodList)
        {
            if (!declaredNames.add(m.getName()))
            {
                invalid(logger, "Overloading method " + m.getName() + " not supported");
            }

            final JParameter[] params = m.getParameters();

            final JType callback = m.getReturnType();

            final JClassType resultType = callback.isParameterized().getTypeArgs()[0];

            for (int i = 0; i < params.length /*- 1*/; i++)
            {
                final JParameter p = params[i];
                final TreeLogger branch = logger.branch(TreeLogger.DEBUG, m.getName() + ", parameter " + p.getName());
                serializerCreator.checkCanSerialize(branch, p.getType());
                if (p.getType().isPrimitive() == null && !SerializerCreator.isBoxedPrimitive(p.getType()))
                {
                    serializerCreator.create((JClassType) p.getType(), branch);
                }
            }

            {
                JClassType p = resultType;
                final TreeLogger branch = logger.branch(TreeLogger.DEBUG, m.getName() + ", result " + p.getName());
                if (p.isPrimitive() == null && !SerializerCreator.isBoxedPrimitive(p))
                {
                    serializerCreator.create((JClassType) p, branch);
                }
            }

            final TreeLogger branch = logger.branch(TreeLogger.DEBUG, m.getName() + ", result " + resultType.getQualifiedSourceName());
            if (resultType.getQualifiedSourceName().startsWith(FutureResult))
            {
                JParameterizedType parameterized = resultType.isParameterized();
                JClassType jClassType = parameterized.getTypeArgs()[0];
                serializerCreator.checkCanSerialize(branch, jClassType);
            }
            else
            {
                serializerCreator.checkCanSerialize(branch, resultType);
            }
            if (resultType.isArray() != null)
            {
                // Arrays need a special deserializer
                deserializerCreator.create(branch, resultType.isArray());
            }
            else if (resultType.isPrimitive() == null && !SerializerCreator.isBoxedPrimitive(resultType))
                // Non primitives get deserialized by their normal serializer
                serializerCreator.create((JClassType) resultType, branch);
            // (Boxed)Primitives are left, they are handled specially
        }
    }

    private boolean returnsCallbackHandle(final JMethod m)
    {
        return m.getReturnType().getErasedType().getQualifiedSourceName().equals(CallbackHandle);
    }

    private void invalid(final TreeLogger logger, final String what) throws UnableToCompleteException
    {
        logger.log(TreeLogger.ERROR, what, null);
        throw new UnableToCompleteException();
    }

    private SourceWriter getSourceWriter(final TreeLogger logger, final GeneratorContext ctx)
    {
        final JPackage servicePkg = svcInf.getPackage();
        final String pkgName = servicePkg == null ? "" : servicePkg.getName();
        final PrintWriter pw;
        final ClassSourceFileComposerFactory cf;

        pw = ctx.tryCreate(logger, pkgName, getProxySimpleName());
        if (pw == null)
        {
            return null;
        }

        cf = new ClassSourceFileComposerFactory(pkgName, getProxySimpleName());
        cf.addImport(AbstractJsonProxy);
        cf.addImport(JsonSerializer);
        cf.addImport(JavaScriptObject.class.getCanonicalName());
        cf.addImport(ResultDeserializer);
        cf.addImport(FutureResultImpl);
        cf.addImport(GWT.class.getCanonicalName());
        cf.setSuperclass(AbstractJsonProxy_simple);
        cf.addImplementedInterface(svcInf.getErasedType().getQualifiedSourceName());
        return cf.createSourceWriter(ctx, pw);
    }

    private void generateProxyConstructor(@SuppressWarnings("unused") final TreeLogger logger, final SourceWriter w)
    {
        final RemoteJsonMethod relPath = svcInf.getAnnotation(RemoteJsonMethod.class);
        if (relPath != null)
        {
            w.println();
            w.println("public " + getProxySimpleName() + "() {");
            w.indent();

            String path = relPath.path();
            if(path == null || path.isEmpty())
            {
                path = svcInf.getErasedType().getQualifiedSourceName();
            }
            w.println("setPath(\"" + path + "\");");
            w.outdent();
            w.println("}");
        }
    }

    private void generateProxyCallCreator(final TreeLogger logger, final SourceWriter w) throws UnableToCompleteException
    {
        String callName = getJsonCallClassName(logger);
        w.println();
        w.println("@Override");
        w.print("protected <T> ");
        w.print(callName);
        w.print("<T> newJsonCall(final AbstractJsonProxy proxy, ");
        w.print("final String methodName, final String reqData, ");
        w.println("final ResultDeserializer<T> ser) {");
        w.indent();

        w.print("return new ");
        w.print(callName);
        w.println("<T>(proxy, methodName, reqData, ser);");

        w.outdent();
        w.println("}");
    }

    private String getJsonCallClassName(final TreeLogger logger) throws UnableToCompleteException
    {
        return JsonCall20HttpPost;
    }

    private void generateProxyMethods(final TreeLogger logger, final SourceWriter srcWriter)
    {
        final JMethod[] methodList = svcInf.getOverridableMethods();
        for (final JMethod m : methodList)
        {
            generateProxyMethod(logger, m, srcWriter);
        }
    }

    private void generateProxyMethod(@SuppressWarnings("unused") final TreeLogger logger, final JMethod method, final SourceWriter w)
    {
        final JParameter[] params = method.getParameters();
        final JType callback = method.getReturnType();// params[params.length - 1];
        JType resultType = callback;
        //    final JClassType resultType =
        //        callback.isParameterized().getTypeArgs()[0];
        final String[] serializerFields = new String[params.length];
        String resultField = "";

        w.println();
        for (int i = 0; i < params.length /*- 1*/; i++)
        {
            final JType pType = params[i].getType();
            if (SerializerCreator.needsTypeParameter(pType))
            {
                serializerFields[i] = "serializer_" + instanceField++;
                w.print("private static final ");
                if (pType.isArray() != null)
                    w.print(serializerCreator.serializerFor(pType));
                else
                    w.print(JsonSerializer);
                w.print(" ");
                w.print(serializerFields[i]);
                w.print(" = ");
                serializerCreator.generateSerializerReference(pType, w, false);
                w.println(";");
            }
        }
        JClassType parameterizedResult = null;
        if (resultType.isParameterized() != null)
        {
            resultField = "serializer_" + instanceField++;
            w.print("private static final ");
            w.print(ResultDeserializer);
            w.print(" ");
            w.print(resultField);
            w.print(" = ");
            parameterizedResult = resultType.isParameterized().getTypeArgs()[0];
            serializerCreator.generateSerializerReference(parameterizedResult, w, false);
            w.println(";");
        }

        w.print("public ");
        w.print(method.getReturnType().getQualifiedSourceName());
        w.print(" ");
        w.print(method.getName());
        w.print("(");
        boolean needsComma = false;
        final NameFactory nameFactory = new NameFactory();
        for (int i = 0; i < params.length; i++)
        {
            final JParameter param = params[i];

            if (needsComma)
            {
                w.print(", ");
            }
            else
            {
                needsComma = true;
            }

            final JType paramType = param.getType().getErasedType();
            w.print(paramType.getQualifiedSourceName());
            w.print(" ");

            nameFactory.addName(param.getName());
            w.print(param.getName());
        }

        w.println(") {");
        w.indent();

        if (returnsCallbackHandle(method))
        {
            w.print("return new ");
            w.print(CallbackHandle);
            w.print("(");
            if (SerializerCreator.needsTypeParameter(resultType))
            {
                w.print(resultField);
            }
            else
            {
                deserializerCreator.generateDeserializerReference(resultType, w);
            }
            w.print(", " + "null" // callback.getName()
            );
            w.println(");");
            w.outdent();
            w.println("}");
            return;
        }

        final String reqDataStr;
        if (params.length == 0)
        {
            reqDataStr = "\"[]\"";
        }
        else
        {
            final String reqData = nameFactory.createName("reqData");
            w.println("final StringBuilder " + reqData + " = new StringBuilder();");
            needsComma = false;
            w.println(reqData + ".append('[');");
            for (int i = 0; i < params.length; i++)
            {
                if (needsComma)
                {
                    w.println(reqData + ".append(\",\");");
                }
                else
                {
                    needsComma = true;
                }

                final JType pType = params[i].getType();
                final String pName = params[i].getName();
                if (pType == JPrimitiveType.CHAR || SerializerCreator.isBoxedCharacter(pType))
                {
                    w.println(reqData + ".append(\"\\\"\");");
                    w.println(reqData + ".append(" + JsonSerializer_simple + ".escapeChar(" + pName + "));");
                    w.println(reqData + ".append(\"\\\"\");");
                }
                else if ((SerializerCreator.isJsonPrimitive(pType) || SerializerCreator.isBoxedPrimitive(pType)) && !SerializerCreator.isJsonString(pType))
                {
                    w.println(reqData + ".append(" + pName + ");");
                }
                else
                {
                    w.println("if (" + pName + " != null) {");
                    w.indent();
                    if (SerializerCreator.needsTypeParameter(pType))
                    {
                        w.print(serializerFields[i]);
                    }
                    else
                    {
                        serializerCreator.generateSerializerReference(pType, w, false);
                    }
                    w.println(".printJson(" + reqData + ", " + pName + ");");
                    w.outdent();
                    w.println("} else {");
                    w.indent();
                    w.println(reqData + ".append(" + JsonSerializer + ".JS_NULL);");
                    w.outdent();
                    w.println("}");
                }
            }
            w.println(reqData + ".append(']');");
            reqDataStr = reqData + ".toString()";
        }

        String resultClass = futureResultClassName;
        if (parameterizedResult != null)
        {
            resultClass += "<" + parameterizedResult.getQualifiedSourceName() + ">";
        }
        w.println(resultClass + " result = new " + resultClass + "();");
        w.print("doInvoke(");
        w.print("\"" + method.getName() + "\"");
        w.print(", " + reqDataStr);
        w.print(", ");
        if (resultType.isParameterized() != null)
        {
            w.print(resultField);
        }
        else
        {
            deserializerCreator.generateDeserializerReference(resultType, w);
        }

        w.print(", result");

        w.println(");");
        w.println("return result;");

        w.outdent();
        w.println("}");
    }

    private String getProxyQualifiedName()
    {
        final String[] name = synthesizeTopLevelClassName(svcInf, PROXY_SUFFIX);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    private String getProxySimpleName()
    {
        return synthesizeTopLevelClassName(svcInf, PROXY_SUFFIX)[1];
    }

    static String[] synthesizeTopLevelClassName(JClassType type, String suffix)
    {
        // Gets the basic name of the type. If it's a nested type, the type name
        // will contains dots.
        //
        String className;
        String packageName;

        JType leafType = type.getLeafType();
        if (leafType.isPrimitive() != null)
        {
            className = leafType.getSimpleSourceName();
            packageName = "";
        }
        else
        {
            JClassType classOrInterface = leafType.isClassOrInterface();
            assert (classOrInterface != null);
            className = classOrInterface.getName();
            packageName = classOrInterface.getPackage().getName();
        }

        JParameterizedType isGeneric = type.isParameterized();
        if (isGeneric != null)
        {
            for (JClassType param : isGeneric.getTypeArgs())
            {
                className += "_";
                className += param.getQualifiedSourceName().replace('.', '_');
            }
        }

        JArrayType isArray = type.isArray();
        if (isArray != null)
        {
            className += "_Array_Rank_" + isArray.getRank();
        }

        // Add the meaningful suffix.
        //
        className += suffix;

        // Make it a top-level name.
        //
        className = className.replace('.', '_');

        return new String[] { packageName, className };
    }
}
