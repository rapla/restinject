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

import org.rapla.gwtjsonrpc.RemoteJsonMethod;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ProxyCreator implements SerializerClasses
{
    private static final String PROXY_SUFFIX = "_JsonProxy";
    private TypeElement svcInf;
    String futureResultClassName;
    private SerializerCreator serializerCreator;
    private org.rapla.gwtjsonrpc.annotation.ResultDeserializerCreator deserializerCreator;
    private int instanceField;
    private final ProcessingEnvironment processingEnvironment;
    private final NameFactory nameFactory = new NameFactory();

    ProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment)
    {
        svcInf = remoteService;
        this.processingEnvironment = processingEnvironment;
    }

    String create(final TreeLogger logger) throws UnableToCompleteException
    {
        serializerCreator = new SerializerCreator(processingEnvironment, nameFactory);
        deserializerCreator = new org.rapla.gwtjsonrpc.annotation.ResultDeserializerCreator(serializerCreator, processingEnvironment);
        futureResultClassName = FutureResultImpl;
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        checkMethods(logger, processingEnvironment);

        final PrintWriter srcWriter = getSourceWriter(logger);
        if (srcWriter == null)
        {
            return getProxyQualifiedName();
        }

        generateProxyConstructor(logger, srcWriter);
        generateProxyCallCreator(logger, srcWriter);
        generateProxyMethods(logger, srcWriter);
        srcWriter.println("};");
        srcWriter.close();

        return getProxyQualifiedName();
    }

    private void checkMethods(final TreeLogger logger, final ProcessingEnvironment processingEnvironment) throws UnableToCompleteException
    {
        final Set<String> declaredNames = new HashSet<String>();
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        for (final ExecutableElement m : methods)
        {
            final String methodName = m.getSimpleName().toString();
            if (!declaredNames.add(methodName))
            {
                invalid(logger, "Overloading method " + methodName + " not supported");
            }
            final List<? extends VariableElement> params = m.getParameters();

            final TypeMirror callback = m.getReturnType();

            final Element ele = processingEnvironment.getTypeUtils().asElement(callback);

            for (VariableElement p : params)
            {
                final TreeLogger branch = logger;//logger.branch(TreeLogger.DEBUG, m.getName() + ", parameter " + p.getName());
                final TypeElement typeP = (TypeElement) processingEnvironment.getTypeUtils().asElement(p.asType());
                serializerCreator.checkCanSerialize(branch, typeP);
                if (SerializerCreator.isPrimitive(typeP) && !SerializerCreator.isBoxedPrimitive(typeP))
                {
                    serializerCreator.create(typeP, branch);
                }
            }
            TypeMirror returnType = m.getReturnType();
            if (SerializerCreator.isPrimitive(returnType))
            {
                continue;
            }
            /*
            {
                final TreeLogger branch = logger;//.branch(TreeLogger.DEBUG, m.getName() + ", result " + p.getName());
                if (!SerializerCreator.isPrimitive(returnType) )
                {
                    if (!SerializerCreator.isBoxedPrimitive(returnType))
                    {
                        final TypeElement resultType = (TypeElement) processingEnvironment.getTypeUtils().asElement(returnType);
                        serializerCreator.create(resultType, branch);
                    }
                }
            }
*/
            final TreeLogger branch = logger;//.branch(TreeLogger.DEBUG, m.getName() + ", result " + resultType.getQualifiedSourceName());
            final Element resultType =  processingEnvironment.getTypeUtils().asElement(returnType);
            if (returnType.toString().startsWith(FutureResult) && resultType instanceof  Parameterizable)
            {
                final List<? extends TypeParameterElement> typeParameters = ((Parameterizable)resultType).getTypeParameters();
                if (typeParameters != null && !typeParameters.isEmpty())
                {
                    final TypeMirror typeMirror = ((DeclaredType) returnType).getTypeArguments().get(0);
                    final Element te = processingEnvironment.getTypeUtils().asElement(typeMirror);
                    serializerCreator.checkCanSerialize(branch, te);
                }
            }
            else
            {
                serializerCreator.checkCanSerialize(branch, resultType);
            }
            if (SerializerCreator.isArray(returnType))
            {
                // Arrays need a special deserializer
                deserializerCreator.create(logger, returnType);
            }
            else if (!SerializerCreator.isPrimitive(returnType) && !SerializerCreator.isBoxedPrimitive(returnType))
            {
                // Non primitives get deserialized by their normal serializer
                TypeElement object = (TypeElement) resultType;
                serializerCreator.create(object, branch);
            }
            // (Boxed)Primitives are left, they are handled specially
        }
    }

    private List<ExecutableElement> getMethods(ProcessingEnvironment processingEnvironment)
    {
        final List<? extends Element> allMembers = processingEnvironment.getElementUtils().getAllMembers(svcInf);
        List<ExecutableElement> result = new ArrayList<ExecutableElement>();
        for (ExecutableElement r: ElementFilter.methodsIn(allMembers))
        {
            if (!canIgnore( r))
            {
                result.add( r );
            }
        }
        return result;
    }

    private boolean canIgnore(ExecutableElement m)
    {
        Element enclosingElement = m.getEnclosingElement();
        Set<Modifier> modifiers = m.getModifiers();
        String methodClass = enclosingElement.asType().toString();
        return modifiers.contains(Modifier.FINAL) || modifiers.contains(Modifier.PRIVATE) || methodClass.equals("java.lang.Object");
    }

    private boolean returnsCallbackHandle(final ExecutableElement m)
    {
        final Element element = processingEnvironment.getTypeUtils().asElement(m.getReturnType());
        final String superName = element.getEnclosingElement().getSimpleName().toString();
        return superName.equals(CallbackHandle);
    }

    private void invalid(final TreeLogger logger, final String what) throws UnableToCompleteException
    {
        logger.error(what);
        throw new UnableToCompleteException(what);
    }

    private PrintWriter getSourceWriter(final TreeLogger logger) throws UnableToCompleteException
    {
        final String pkgName = processingEnvironment.getElementUtils().getPackageOf(svcInf).toString();
        PrintWriter pw = null;

        final String className = svcInf.getSimpleName().toString() + PROXY_SUFFIX;
        try
        {
            //String pathname = pkgName.replaceAll("\\.", "/") + "/" + className;
            //File file = new File(pathname);
            String name = svcInf.getQualifiedName() + PROXY_SUFFIX;
            JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(name,svcInf);
            pw = new PrintWriter(sourceFile.openWriter());
        }
        catch (IOException e)
        {
            throw new UnableToCompleteException(e.getMessage());
        }
        pw.println("package " + pkgName + ";");
        pw.println("import " + AbstractJsonProxy + ";");
        pw.println("import " + JsonSerializer + ";");
        pw.println("import " + JavaScriptObject.class.getCanonicalName() + ";");
        pw.println("import " + ResultDeserializer + ";");
        pw.println("import " + FutureResultImpl + ";");
        pw.println("import " + GWT.class.getCanonicalName() + ";");
        pw.println();
        TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
        String interfaceName =  erasedType.getQualifiedName().toString();
        pw.println("public class " + className + " extends " + AbstractJsonProxy_simple + " implements " + interfaceName);
        pw.println("{");
        return pw;
    }

    private void generateProxyConstructor(@SuppressWarnings("unused") final TreeLogger logger, final PrintWriter w)
    {
        final RemoteJsonMethod relPath = svcInf.getAnnotation(RemoteJsonMethod.class);
        if (relPath != null)
        {
            w.println();
            w.println("public " + getProxySimpleName() + "() {");
            //w.indent();

            String path = relPath.path();
            if (path == null || path.isEmpty())
            {
                TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
                path = erasedType.getQualifiedName().toString();
            }
            w.println("setPath(\"" + path + "\");");
            //w.outdent();
            w.println("}");
        }
    }

    private void generateProxyCallCreator(final TreeLogger logger, final PrintWriter w) throws UnableToCompleteException
    {
        String callName = getJsonCallClassName(logger);
        w.println();
        w.println("@Override");
        w.print("protected <T> ");
        w.print(callName);
        w.print("<T> newJsonCall(final AbstractJsonProxy proxy, ");
        w.print("final String methodName, final String reqData, ");
        w.println("final ResultDeserializer<T> ser) {");
        //w.indent();

        w.print("return new ");
        w.print(callName);
        w.println("<T>(proxy, methodName, reqData, ser);");

        //w.outdent();
        w.println("}");
    }

    private String getJsonCallClassName(final TreeLogger logger) throws UnableToCompleteException
    {
        return JsonCall20HttpPost;
    }

    private void generateProxyMethods(final TreeLogger logger, final PrintWriter srcWriter)
    {
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        for (final ExecutableElement m : methods)
        {
            generateProxyMethod(logger, m, srcWriter);
        }
    }

    private void generateProxyMethod(@SuppressWarnings("unused") final TreeLogger logger, final ExecutableElement method, final PrintWriter w)
    {
        final List<? extends VariableElement> params = method.getParameters();
        final TypeElement callback = (TypeElement) processingEnvironment.getTypeUtils().asElement(method.getReturnType());// params[params.length - 1];
        TypeElement resultType = callback;
        //    final JClassType resultType =
        //        callback.isParameterized().getTypeArgs()[0];
        final String[] serializerFields = new String[params.size()];
        String resultField = "";

        w.println();
        for (int i = 0; i < params.size() /*- 1*/; i++)
        {
            final TypeElement pType = (TypeElement) processingEnvironment.getTypeUtils().asElement(params.get(i).asType());
            if (SerializerCreator.needsTypeParameter(pType))
            {
                serializerFields[i] = "serializer_" + instanceField++;
                w.print("private static final ");
                if (SerializerCreator.isArray(pType))
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
        TypeParameterElement parameterizedResult = null;
        final List<? extends TypeParameterElement> typeParameters = resultType.getTypeParameters();
        if (typeParameters != null && !typeParameters.isEmpty())
        {
            resultField = "serializer_" + instanceField++;
            w.print("private static final ");
            w.print(ResultDeserializer);
            w.print(" ");
            w.print(resultField);
            w.print(" = ");
            parameterizedResult = typeParameters.get(0);
            TypeElement te = (TypeElement) processingEnvironment.getTypeUtils().asElement(parameterizedResult.asType());
            serializerCreator.generateSerializerReference(te, w, false);
            w.println(";");
        }

        w.print("public ");

        w.print(processingEnvironment.getTypeUtils().asElement(method.getReturnType()).getSimpleName().toString());
        w.print(" ");
        w.print(method.getSimpleName().toString());
        w.print("(");
        boolean needsComma = false;
        for (int i = 0; i < params.size(); i++)
        {
            final VariableElement param = params.get(i);
            String pname = param.getSimpleName().toString();
            if (needsComma)
            {
                w.print(", ");
            }
            else
            {
                needsComma = true;
            }
            final TypeElement paramType = (TypeElement) processingEnvironment.getTypeUtils().asElement(param.asType());
            w.print(paramType.getQualifiedName().toString());
            w.print(" ");

            nameFactory.addName(pname);
            w.print(pname);
        }

        w.println(") {");
        //w.indent();

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
            //w.outdent();
            w.println("}");
            return;
        }

        final String reqDataStr;
        if (params.isEmpty())
        {
            reqDataStr = "\"[]\"";
        }
        else
        {
            final String reqData = nameFactory.createName("reqData");
            w.println("final StringBuilder " + reqData + " = new StringBuilder();");
            needsComma = false;
            w.println(reqData + ".append('[');");
            for (int i = 0; i < params.size(); i++)
            {
                if (needsComma)
                {
                    w.println(reqData + ".append(\",\");");
                }
                else
                {
                    needsComma = true;
                }

                final VariableElement param = params.get(i);
                String pName = param.getSimpleName().toString();

                TypeMirror paramType = param.asType();
                //                pType == JPrimitiveType.CHAR ||
                if (SerializerCreator.isBoxedCharacter(paramType))
                {
                    w.println(reqData + ".append(\"\\\"\");");
                    w.println(reqData + ".append(" + JsonSerializer_simple + ".escapeChar(" + pName + "));");
                    w.println(reqData + ".append(\"\\\"\");");
                }
                else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType)) && !SerializerCreator.isJsonString(paramType))
                {
                    w.println(reqData + ".append(" + pName + ");");
                }
                else
                {
                    final TypeElement pType = (TypeElement) processingEnvironment.getTypeUtils().asElement(paramType);
                    w.println("if (" + pName + " != null) {");
                    //w.indent();
                    if (SerializerCreator.needsTypeParameter(pType))
                    {
                        w.print(serializerFields[i]);
                    }
                    else
                    {
                        serializerCreator.generateSerializerReference(pType, w, false);
                    }
                    w.println(".printJson(" + reqData + ", " + pName + ");");
                    //w.outdent();
                    w.println("} else {");
                    //w.indent();
                    w.println(reqData + ".append(" + JsonSerializer + ".JS_NULL);");
                    //w.outdent();
                    w.println("}");
                }
            }
            w.println(reqData + ".append(']');");
            reqDataStr = reqData + ".toString()";
        }

        String resultClass = futureResultClassName;
        if (parameterizedResult != null)
        {
            TypeElement te = ((TypeElement)processingEnvironment.getTypeUtils().asElement(parameterizedResult.asType()));
            resultClass += "<" + te.getQualifiedName().toString() + ">";
        }
        w.println(resultClass + " result = new " + resultClass + "();");
        w.print("doInvoke(");
        w.print("\"" + method.getSimpleName().toString() + "\"");
        w.print(", " + reqDataStr);
        w.print(", ");
        final List<? extends TypeParameterElement> resultTypeParameters = resultType.getTypeParameters();
        if (resultTypeParameters != null && !resultTypeParameters.isEmpty())
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

        //w.outdent();
        w.println("}");
    }

    private String getProxyQualifiedName()
    {
        final String[] name = synthesizeTopLevelClassName(svcInf, PROXY_SUFFIX, nameFactory, processingEnvironment);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    private String getProxySimpleName()
    {
        return synthesizeTopLevelClassName(svcInf, PROXY_SUFFIX, nameFactory, processingEnvironment)[1];
    }

    static String[] synthesizeTopLevelClassName(TypeElement type, String suffix, NameFactory nameFactory, ProcessingEnvironment processingEnvironment)
    {
        // Gets the basic name of the type. If it's a nested type, the type name
        // will contains dots.
        //
        String className;
        String packageName;

        TypeElement leafType = type;
        if (SerializerCreator.isPrimitive(leafType))
        {
            className = leafType.getSimpleName().toString();
            packageName = "";
        }
        else
        {
            assert(SerializerCreator.isClass(leafType) || SerializerCreator.isInterface(leafType));
            className = leafType.getSimpleName().toString();
            packageName = leafType.getQualifiedName().subSequence(0, leafType.getQualifiedName().toString().lastIndexOf(".")).toString();
        }

        boolean isGeneric = SerializerCreator.isParameterized(type);
        if (isGeneric)
        {
            final List<? extends TypeParameterElement> typeParameters = type.getTypeParameters();
            for (TypeParameterElement param : typeParameters)
            {
                className += "_";
                className += ((TypeElement) processingEnvironment.getTypeUtils().asElement(param.asType())).getQualifiedName().toString().replace('.', '_');
            }
        }

        boolean isArray = SerializerCreator.isArray(type);
        if (isArray)
        {
            className += "_Array_Rank_";

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
