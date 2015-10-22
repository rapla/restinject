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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.*;

public class SwingProxyCreator implements SerializerClasses
{
    private static final String PROXY_SUFFIX = "_JavaJsonProxy";
    private TypeElement svcInf;
    String futureResultClassName;
    private SerializerCreator serializerCreator;
    private ResultDeserializerCreator deserializerCreator;
    private int instanceField;
    private final ProcessingEnvironment processingEnvironment;
    private final NameFactory nameFactory = new NameFactory();
    String generatorName;

    public SwingProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        svcInf = remoteService;
        this.generatorName = generatorName;
        this.processingEnvironment = processingEnvironment;
        serializerCreator = new SerializerCreator(processingEnvironment, nameFactory, generatorName);
        deserializerCreator = new ResultDeserializerCreator(serializerCreator, processingEnvironment, generatorName);
        futureResultClassName = FutureResultImpl;
    }

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    public String create(final TreeLogger logger) throws UnableToCompleteException
    {
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        checkMethods(logger, processingEnvironment);

        final SourceWriter srcWriter = getSourceWriter(logger);
        if (srcWriter == null)
        {
            return getProxyQualifiedName();
        }

        generateProxyConstructor(logger, srcWriter);
        //generateProxyCallCreator(logger, srcWriter);
        generateProxyMethods(logger, srcWriter);
        srcWriter.outdent();
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
                //final TypeElement typeP = (TypeElement) processingEnvironment.getTypeUtils().asElement(p.asType());
                TypeMirror typeP = p.asType();
                serializerCreator.checkCanSerialize(branch, typeP);
                if (!SerializerCreator.isPrimitive(typeP) && !SerializerCreator.isBoxedPrimitive(typeP))
                {
                    //serializerCreator.create(typeP, branch);
                }
            }
            TypeMirror returnType = m.getReturnType();
            if (SerializerCreator.isPrimitive(returnType))
            {
                continue;
            }

            final TreeLogger branch = logger;//.branch(TreeLogger.DEBUG, m.getName() + ", result " + resultType.getQualifiedSourceName());
            if (returnType.toString().startsWith(FutureResult) && SerializerCreator.isParameterized(returnType))
            {
                final TypeMirror typeMirror = getParameters(returnType).get(0);
                serializerCreator.checkCanSerialize(branch, typeMirror);
                returnType = typeMirror;
            }
            else
            {
                serializerCreator.checkCanSerialize(branch, returnType);
            }
            if (SerializerCreator.isArray(returnType))
            {
                // Arrays need a special deserializer
                //deserializerCreator.create(logger, returnType);
            }
            else if (!SerializerCreator.isPrimitive(returnType) && !SerializerCreator.isBoxedPrimitive(returnType))
            {
                // Non primitives get deserialized by their normal serializer
                //serializerCreator.create(returnType, branch);
            }
            // (Boxed)Primitives are left, they are handled specially
        }
    }

    private List<? extends TypeMirror> getParameters(TypeMirror returnType)
    {
        if (!(returnType instanceof DeclaredType))
        {
            return Collections.emptyList();
        }
        return ((DeclaredType) returnType).getTypeArguments();
    }

    private List<ExecutableElement> getMethods(ProcessingEnvironment processingEnvironment)
    {
        final List<? extends Element> allMembers = processingEnvironment.getElementUtils().getAllMembers(svcInf);
        List<ExecutableElement> result = new ArrayList<ExecutableElement>();
        for (ExecutableElement r : ElementFilter.methodsIn(allMembers))
        {
            if (!canIgnore(r))
            {
                result.add(r);
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

    private void invalid(final TreeLogger logger, final String what) throws UnableToCompleteException
    {
        logger.error(what);
        throw new UnableToCompleteException(what);
    }

    private SourceWriter getSourceWriter(final TreeLogger logger) throws UnableToCompleteException
    {
        final String pkgName = processingEnvironment.getElementUtils().getPackageOf(svcInf).getQualifiedName().toString();
        SourceWriter pw = null;

        final String className = svcInf.getSimpleName().toString() + PROXY_SUFFIX;
        try
        {
            //String pathname = pkgName.replaceAll("\\.", "/") + "/" + className;
            //File file = new File(pathname);
            String name = svcInf.getQualifiedName() + PROXY_SUFFIX;
            JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(name, svcInf);
            pw = new SourceWriter(sourceFile.openWriter());
        }
        catch (IOException e)
        {
            throw new UnableToCompleteException(e.getMessage());
        }
        pw.println("package " + pkgName + ";");
        pw.println("import java.util.concurrent.Executor;");
        pw.println("import java.lang.reflect.Method;");
        pw.println("import com.google.gson.JsonObject;");
        pw.println("import java.net.URL;");
        pw.println("import " + FutureResult + ";");
        pw.println("import " + AbstractJsonJavaProxy + ";");
        pw.println("import " + FutureResultImpl + ";");

        pw.println();
        TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
        String interfaceName = erasedType.getQualifiedName().toString();
        pw.println(getGeneratorString());
        pw.println("public class " + className + " extends " + AbstractJsonJavaProxy + " implements " + interfaceName);
        pw.println("{");
        pw.indent();
        return pw;
    }

    private void generateProxyConstructor(@SuppressWarnings("unused") final TreeLogger logger, final SourceWriter w)
    {
        final RemoteJsonMethod relPath = svcInf.getAnnotation(RemoteJsonMethod.class);
        if (relPath != null)
        {
            w.println();
            w.println("public " + getProxySimpleName() + "(Executor scheduler, String connectErrorString) {");
            w.indent();
            w.println("super(scheduler, connectErrorString);");
            String path = relPath.path();
            if (path == null || path.isEmpty())
            {
                TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
                path = erasedType.getQualifiedName().toString();
            }
            w.println("setPath(\"" + path + "\");");
            w.outdent();
            w.println("}");
        }
    }

    private String getJsonCallClassName(final TreeLogger logger) throws UnableToCompleteException
    {
        return JsonCall20HttpPost;
    }

    private void generateProxyMethods(final TreeLogger logger, final SourceWriter srcWriter)
    {
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        for (final ExecutableElement m : methods)
        {
            generateProxyMethod(logger, m, srcWriter);
        }
    }

    private void generateProxyMethod(@SuppressWarnings("unused") final TreeLogger logger, final ExecutableElement method, final SourceWriter w)
    {

        final List<? extends VariableElement> params = method.getParameters();
        final TypeMirror callback = method.getReturnType();// params[params.length - 1];
        TypeMirror resultType = callback;
        //    final JClassType resultType =
        //        callback.isParameterized().getTypeArgs()[0];
        final String[] serializerFields = new String[params.size()];
        String resultField = "";

        w.println();
        //        for (int i = 0; i < params.size() /*- 1*/; i++)
        //        {
        //            final VariableElement variableElement = params.get(i);
        //            final TypeMirror pType = variableElement.asType();
        //            if (SerializerCreator.needsTypeParameter(pType, processingEnvironment))
        //            {
        //                serializerFields[i] = "serializer_" + instanceField++;
        //                w.print("private static final ");
        //                if (SerializerCreator.isArray(pType))
        //                    w.print(serializerCreator.serializerFor(pType));
        //                else
        //                    w.print(JsonSerializer);
        //                w.print(" ");
        //                w.print(serializerFields[i]);
        //                w.print(" = ");
        //                serializerCreator.generateSerializerReference(pType, w, false);
        //                w.println(";");
        //            }
        //        }
        TypeMirror parameterizedResult = null;
        if (SerializerCreator.isParameterized(resultType))
        {
            resultField = "serializer_" + instanceField++;
            //            w.print("private static final ");
            //            w.print(ResultDeserializer);
            //            w.print(" ");
            //            w.print(resultField);
            //            w.print(" = ");
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) resultType).getTypeArguments();
            parameterizedResult = typeArguments.get(0);
            //            serializerCreator.generateSerializerReference(parameterizedResult, w, false);
            //            w.println(";");
        }

        w.print("public ");

        w.print(method.getReturnType().toString());
        w.print(" ");
        final String methodName = method.getSimpleName().toString();
        w.print(methodName);
        w.print("(");
        boolean needsComma = false;
        StringBuilder argsBuilder = new StringBuilder();
        for (int i = 0; i < params.size(); i++)
        {
            final VariableElement param = params.get(i);
            String pname = param.getSimpleName().toString();
            if (needsComma)
            {
                w.print(", ");
                argsBuilder.append(", ");
            }
            else
            {
                needsComma = true;
            }
            //final TypeElement paramType = (TypeElement) processingEnvironment.getTypeUtils().asElement(param.asType());
            w.print(param.asType().toString());
            w.print(" ");

            nameFactory.addName(pname);
            w.print(pname);
            argsBuilder.append(pname);
        }

        w.println(") {");
        w.indent();
        final boolean wrapFutureResult;
        final String resultClassname;
        String containerClass = "null";
        {
            if ((resultType instanceof DeclaredType) && ((DeclaredType) resultType).getTypeArguments() != null && !((DeclaredType) resultType)
                    .getTypeArguments().isEmpty())
            {
                final DeclaredType resultType1 = (DeclaredType) resultType;
                final List<? extends TypeMirror> typeArguments = resultType1.getTypeArguments();
                wrapFutureResult = resultType1.toString().startsWith(FutureResult);
                if ( !wrapFutureResult)
                {
                    TypeMirror erasedType = SerializerCreator.getErasedType(resultType1, processingEnvironment);
                    containerClass = "\"" +erasedType.toString() + "\"";
                    resultClassname = typeArguments.get(0).toString();
                }
                else
                {
                    if (typeArguments.size() == 1)
                    {
                        containerClass = "null";
                        resultClassname = typeArguments.get(0).toString();
                    }
                    else
                    {
                        containerClass = typeArguments.get(0).toString();
                        resultClassname = typeArguments.get(1).toString();
                    }
                }

                {

//                    if (resultType1.toString().startsWith("java.util.List"))
//                    {
//                        containerClass = "\"java.util.List\"";
//                    }
                }

            }
            else
            {
                resultClassname = resultType.toString();
                wrapFutureResult = false;
            }
        }
        w.println("try{");
        w.indent();
        if (wrapFutureResult)
        {
            w.println("return new BasicRaplaHTTPConnector.MyFutureResult() {");
            w.indent();
            w.println("@Override public Object get() throws Exception {");
            w.indent();
        }

        String resultClass = futureResultClassName;
        if (parameterizedResult != null)
        {
            resultClass += "<" + parameterizedResult.toString() + ">";
        }

        w.indent();
        final String className = svcInf.getQualifiedName().toString();
        w.println("URL methodURL = getMethodUrl(\"" + className + "\", \"" + methodName + "\");");
        w.println("String accessToken = null;");
        //final String resultClassname = "org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest.Result";

        w.println("Method remoteMethod = findMethod(" + className + ".class, \"" + methodName + "\");");
        w.println("Object[] args = new Object[] { " + argsBuilder.toString() + " };");

        w.println("final JsonObject element = serializeCall(remoteMethod, args);");
        w.println("JsonObject resultMessage = sendCall_(\"POST\", methodURL, element, accessToken);");
        w.println("Class resultType = " + resultClassname + ".class;");
        w.println("Class containerClass = " + containerClass + ";");
        w.println("final Object result = getResult(resultMessage, resultType, containerClass);");
        if ( !"void".equals(resultClassname))
        {
            w.println("return (" + resultClassname + ") result;");
        }

        if (wrapFutureResult)
        {
            w.outdent();
            w.println("}");
            w.outdent();
            w.println("};");
        }

        w.outdent();
        w.println("} catch (Exception ex) {");
        w.indent();
        w.println("throw new RuntimeException(ex);");
        w.outdent();
        w.println("}");
        w.outdent();
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

        TypeMirror typeMirror = getLeafType(type.asType());
        TypeElement leafType = (TypeElement) processingEnvironment.getTypeUtils().asElement(typeMirror);
        if (SerializerCreator.isPrimitive(typeMirror))
        {
            className = leafType.getSimpleName().toString();
            packageName = "";
        }
        else
        {
            //assert(SerializerCreator.isClass(typeMirror) || SerializerCreator.isInterface(typeMirror));
            packageName = processingEnvironment.getElementUtils().getPackageOf(processingEnvironment.getTypeUtils().asElement(typeMirror)).getQualifiedName()
                    .toString();
            className = typeMirror.toString().substring(packageName.length() + 1);

        }

        boolean isGeneric = SerializerCreator.isParameterized(typeMirror);
        if (isGeneric)
        {
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
            for (TypeMirror param : typeArguments)
            {
                className += "_";
                className += param.toString().replace('.', '_');
            }
        }

        boolean isArray = SerializerCreator.isArray(typeMirror);
        if (isArray)
        {
            int rank = getRank((ArrayType) typeMirror);
            className += "_Array_Rank_" + rank;

        }

        // Add the meaningful suffix.
        //
        className += suffix;

        // Make it a top-level name.
        //
        className = className.replace('.', '_');

        return new String[] { packageName, className };
    }

    private static TypeMirror getLeafType(TypeMirror type)
    {
        if (type instanceof ArrayType)
        {
            final TypeMirror componentType = ((ArrayType) type).getComponentType();
            return getLeafType(componentType);
        }
        return type;
    }

    public static int getRank(ArrayType typeMirror)
    {
        final TypeMirror componentType = typeMirror.getComponentType();
        if (typeMirror == componentType)
        {
            return 1;
        }
        if (componentType instanceof ArrayType)
        {
            return getRank((ArrayType) componentType) + 1;
        }
        return 1;
    }
}
