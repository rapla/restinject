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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaFileObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.rapla.inject.generator.internal.SourceWriter;
import org.rapla.rest.client.swing.HTTPConnector.HttpCallResult;

public class JavaClientProxyCreator implements SerializerClasses
{
    public static final String PROXY_SUFFIX = "_JavaJsonProxy";
    private TypeElement svcInf;
    private SerializerCreator serializerCreator;
    private ResultDeserializerCreator deserializerCreator;
    private int instanceField;
    private final ProcessingEnvironment processingEnvironment;
    private final NameFactory nameFactory = new NameFactory();
    String generatorName;

    public JavaClientProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        svcInf = remoteService;
        this.generatorName = generatorName;
        this.processingEnvironment = processingEnvironment;
        serializerCreator = new SerializerCreator(processingEnvironment, nameFactory, generatorName);
        deserializerCreator = new ResultDeserializerCreator(serializerCreator, processingEnvironment, generatorName);
    }

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    public String create(final TreeLogger logger) throws UnableToCompleteException
    {
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        checkMethods(logger, processingEnvironment);

        TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
        String interfaceName = erasedType.getQualifiedName().toString();

        final SourceWriter srcWriter = getSourceWriter(logger, interfaceName);
        if (srcWriter == null)
        {
            return getProxyQualifiedName();
        }

        generateProxyConstructor(logger, srcWriter, interfaceName);
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
            serializerCreator.checkCanSerialize(branch, returnType);
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

    private SourceWriter getSourceWriter(final TreeLogger logger, String interfaceName) throws UnableToCompleteException
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
        pw.println("import com.google.gson.JsonElement;");
        pw.println("import java.net.URL;");
        pw.println("import " + HttpCallResult.class.getCanonicalName() + ";");
        pw.println("import " + Map.class.getCanonicalName() + ";");
        pw.println("import " + HashMap.class.getCanonicalName() + ";");
        pw.println("import " + AbstractJsonJavaProxy + ";");
        pw.println("import " + AbstractJsonJavaProxy + ".CustomConnector;");

        pw.println();

        pw.println(getGeneratorString());
        //pw.println("@" + DefaultImplementation.class.getCanonicalName() + "(of=" + interfaceName + ".class, context=" + InjectionContext.class.getCanonicalName() + "." + InjectionContext.swing + ")");
        pw.println("public class " + className + " extends " + AbstractJsonJavaProxy + " implements " + interfaceName);
        pw.println("{");
        pw.indent();
        return pw;
    }

    private void generateProxyConstructor(@SuppressWarnings("unused") final TreeLogger logger, final SourceWriter w, String interfaceName)
    {
        final Path relPath = svcInf.getAnnotation(Path.class);
        if (relPath != null)
        {
            w.println();
            w.println("@javax.inject.Inject");
            w.println("public " + getProxySimpleName() + "(CustomConnector customConnector) {");
            w.indent();
            w.println("super(customConnector);");
            //            TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
            String path = relPath.value();
            w.println("setPath(\"" + path + "\");");
            w.outdent();
            w.println("}");
        }

        w.println(interfaceName + " createMock(){");
        w.indent();
        w.println("return getMockProxy().create(" + interfaceName + ".class, getMockAccessToken());");
        w.outdent();
        w.println("}");

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
            w.print("final ");
            //final TypeElement paramType = (TypeElement) processingEnvironment.getTypeUtils().asElement(param.asType());
            w.print(param.asType().toString());
            w.print(" ");

            nameFactory.addName(pname);
            w.print(pname);
            argsBuilder.append(pname);
        }

        w.println(") {");
        w.indent();
        final String resultClassname;
        final String cast;
        String containerClass = "null";
        {
            if ((resultType instanceof DeclaredType) && ((DeclaredType) resultType).getTypeArguments() != null
                    && !((DeclaredType) resultType).getTypeArguments().isEmpty())
            {
                final DeclaredType resultType1 = (DeclaredType) resultType;
                final List<? extends TypeMirror> typeArguments = resultType1.getTypeArguments();
                TypeMirror typeMirror = resultType1;
                {
                    TypeMirror erasedType = SerializerCreator.getErasedType(typeMirror, processingEnvironment);
                    containerClass = erasedType.toString() + ".class";
                }
                {
                    final List<? extends TypeMirror> typeArguments1 = ((DeclaredType) typeMirror).getTypeArguments();
                    final TypeMirror innerType = typeArguments1.get(typeArguments1.size() - 1);
                    final TypeMirror erasedType = SerializerCreator.getErasedType(innerType, processingEnvironment);
                    resultClassname = erasedType.toString();
                }
                cast = typeMirror.toString();

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
                cast = resultClassname;
            }
        }
        final boolean hasReturn = !"void".equals(resultClassname);
        w.println("try{");
        w.indent();
        final String s = "return";
        w.println("if ( isMock() ) {" + (hasReturn ? "return" : "") + " createMock()." + methodName + "(" + argsBuilder.toString() + "); }");

        w.println("java.lang.String subPath = \"" + (method.getAnnotation(Path.class) != null ? method.getAnnotation(Path.class).value() : "") + "\";");
        w.println("final Map<java.lang.String, java.lang.String>additionalHeaders = new HashMap<>();");
        final String className = svcInf.getQualifiedName().toString();
        boolean firstQueryParam = true;
        boolean elementPrint = false;
        for (int i = 0; i < params.size(); i++)
        {
            final VariableElement param = params.get(i);
            final TypeMirror paramType = param.asType();
            String pname = param.getSimpleName().toString();
            final QueryParam queryAnnotation = param.getAnnotation(QueryParam.class);
            final PathParam pathAnnotation = param.getAnnotation(PathParam.class);
            final HeaderParam headerAnnotation = param.getAnnotation(HeaderParam.class);
            final FormParam formAnnotation = param.getAnnotation(FormParam.class);
            if (queryAnnotation != null || pathAnnotation != null || headerAnnotation != null || formAnnotation != null)
            {
                w.println("final String param" + i + ";");
                w.println("{");
                w.indent();
                if (SerializerCreator.isBoxedCharacter(paramType))
                {
                    w.println("param" + i + " = " + pname + " != null ? " + pname + ".toString() : null;");
                }
                else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType))
                        && !SerializerCreator.isJsonString(paramType))
                {
                    w.println("param" + i + " = " + pname + "+\"\";");
                }
                else if (SerializerCreator.isArray(paramType))
                {
                    w.println("{");
                    w.indent();
                    w.println("boolean first = true;");
                    w.println(StringBuilder.class.getCanonicalName() + " sb = new " + StringBuilder.class.getCanonicalName() + "();");
                    w.println("for(int i = 0;i < "+pname+".length; i++) {");
                    w.println("if(first) {");
                    w.indent();
                    w.println("first = false;");
                    w.outdent();
                    w.println("} else {");
                    w.indent();
                    String name = (queryAnnotation != null ? queryAnnotation.value(): (headerAnnotation != null ? headerAnnotation.value() : formAnnotation.value())); 
                    w.println("sb.append(\"&"+name+"=\");");
                    w.outdent();
                    w.println("} ");
                    w.indent();
                    w.println("sb.append("+pname+"[i]);");
                    w.outdent();
                    w.println("}");
                    w.outdent();
                    w.println("param" + i + " = sb.toString();");
                    w.println("}");
                }
                else if (processingEnvironment.getTypeUtils().isAssignable(paramType, processingEnvironment.getTypeUtils()
                        .getDeclaredType(processingEnvironment.getElementUtils().getTypeElement(Collection.class.getCanonicalName()))))
                {
                    w.println("{");
                    w.indent();
                    w.println("boolean first = true;");
                    w.println(StringBuilder.class.getCanonicalName() + " sb = new " + StringBuilder.class.getCanonicalName() + "();");
                    w.println("for(" + Iterator.class.getCanonicalName() + " it = " + pname + ".iterator(); it.hasNext(); ) {");
                    w.println("if(first) {");
                    w.indent();
                    w.println("first = false;");
                    w.outdent();
                    w.println("} else {");
                    w.indent();
                    String name = (queryAnnotation != null ? queryAnnotation.value(): (headerAnnotation != null ? headerAnnotation.value() : formAnnotation.value())); 
                    w.println("sb.append(\"&"+name+"=\");");
                    w.outdent();
                    w.println("} ");
                    w.indent();
                    w.println("sb.append(it.next());");
                    w.outdent();
                    w.println("}");
                    w.outdent();
                    w.println("param" + i + " = sb.toString();");
                    w.println("}");
                }
                else
                {
                    w.println("param" + i + " = " + pname + " != null ? " + pname + ".toString() : \"\";");
                }
                w.outdent();
                w.println("}");
            }

            if (queryAnnotation != null)
            {
                if (firstQueryParam)
                {
                    w.print("subPath += \"?");
                    firstQueryParam = false;
                }
                else
                {
                    w.print("subPath += \"&");
                }
                w.println(queryAnnotation.value() + "=\"+param" + i + ";");
                continue;
            }
            if (pathAnnotation != null)
            {
                final String pathVariable = "{" + pathAnnotation.value() + "}";
                w.println("subPath = subPath.replaceFirst(\"" + pathVariable + "\", param" + i + ");");
                continue;
            }
            if (headerAnnotation != null)
            {
                w.println("additionalHeaders.put(\"" + headerAnnotation.value() + "\", param" + i + ");");
                continue;
            }
            if (formAnnotation != null)
            {
                w.println("additionalHeaders.put(\"" + formAnnotation.value() + "\", param" + i + ");");
                continue;
            }
            w.println("final JsonElement element = serializeCall(" + pname + ");");
            elementPrint = true;
        }
        if (!elementPrint)
        {
            w.println("final JsonElement element = null;");
        }
        w.println("URL methodURL = getMethodUrl(\"" + className + "\", subPath);");
        //final String resultClassname = "org.rapla.gwtjsonrpc.proxy.AnnotationProcessingTest.Result";

        //        w.println("Method remoteMethod = findMethod(" + className + ".class, \"" + methodName + "\");");
        //        w.println("Object[] args = new Object[] { " + argsBuilder.toString() + " };");
        final String methodType;
        if (method.getAnnotation(POST.class) != null)
        {
            methodType = "POST";
        }
        else if (method.getAnnotation(PUT.class) != null)
        {
            methodType = "PUT";
        }
        else if (method.getAnnotation(DELETE.class) != null)
        {
            methodType = "DELETE";
        }
        else // if (method.getAnnotation(GET.class) != null)
        {
            methodType = "GET";
        }
        w.println("HttpCallResult resultMessage = sendCall_(\"" + methodType + "\", methodURL, element, additionalHeaders);");
        w.println("Class resultType = " + resultClassname + ".class;");
        w.println("Class containerClass = " + containerClass + ";");
        w.println("final Object result = getResult(resultMessage, resultType, containerClass);");
        if (hasReturn)
        {
            w.println("return (" + cast + ") result;");
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
