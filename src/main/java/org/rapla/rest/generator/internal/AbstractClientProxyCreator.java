package org.rapla.rest.generator.internal;

import org.rapla.inject.generator.internal.SourceWriter;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.swing.HTTPConnector;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractClientProxyCreator implements SerializerClasses
{
    protected final ProcessingEnvironment processingEnvironment;
    protected final NameFactory nameFactory = new NameFactory();
    protected TypeElement svcInf;
    protected SerializerCreator serializerCreator;
    protected ResultDeserializerCreator deserializerCreator;
    String generatorName;

    public AbstractClientProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        serializerCreator = new SerializerCreator(processingEnvironment, nameFactory, generatorName);
        deserializerCreator = new ResultDeserializerCreator(serializerCreator, processingEnvironment, generatorName);
        this.processingEnvironment = processingEnvironment;
        this.generatorName = generatorName;
        svcInf = remoteService;
    }

    protected String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    public String create(final TreeLogger logger) throws UnableToCompleteException
    {
        checkMethods(logger, processingEnvironment);

        TypeElement erasedType = SerializerCreator.getErasedType(svcInf, processingEnvironment);
        String interfaceName = erasedType.getQualifiedName().toString();

        final SourceWriter srcWriter = getSourceWriter(logger, interfaceName);
        if (srcWriter == null)
        {
            return getProxyQualifiedName();
        }

        generateProxyConstructor(logger, srcWriter, interfaceName);
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

            for (VariableElement p : params)
            {
                final TreeLogger branch = logger;//logger.branch(TreeLogger.DEBUG, m.getName() + ", parameter " + p.getName());
                //final TypeElement typeP = (TypeElement) processingEnvironment.getTypeUtils().asElement(p.asType());
                TypeMirror typeP = p.asType();
                serializerCreator.checkCanSerialize(branch, typeP);
                if (!SerializerCreator.isPrimitive(typeP) && !SerializerCreator.isBoxedPrimitive(typeP))
                {
                    serializerCreator.create(typeP, branch);
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
                deserializerCreator.create(logger, returnType);
            }
            else if (!SerializerCreator.isPrimitive(returnType) && !SerializerCreator.isBoxedPrimitive(returnType) && returnType.getKind() != TypeKind.VOID)
            {
                // Non primitives get deserialized by their normal serializer
                serializerCreator.create(returnType, branch);
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

    protected SourceWriter getSourceWriter(final TreeLogger logger, String interfaceName) throws UnableToCompleteException
    {
        final String pkgName = processingEnvironment.getElementUtils().getPackageOf(svcInf).getQualifiedName().toString();
        SourceWriter pw;
        final String className = svcInf.getSimpleName().toString() + getProxySuffix();
        try
        {
            //String pathname = pkgName.replaceAll("\\.", "/") + "/" + className;
            //File file = new File(pathname);
            String name = svcInf.getQualifiedName() + getProxySuffix();
            JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(name, svcInf);
            pw = new SourceWriter(sourceFile.openWriter());
        }
        catch (IOException e)
        {
            throw new UnableToCompleteException(e.getMessage());
        }
        pw.println("package " + pkgName + ";");
        pw.println("import " + Map.class.getCanonicalName() + ";");
        pw.println("import " + HashMap.class.getCanonicalName() + ";");
        pw.println("import " + CustomConnector.class.getCanonicalName() + ";");
        final String superClass = writeImports(pw);
        pw.println();
        pw.println(getGeneratorString());
        pw.println("public class " + className + " extends " + superClass + " implements " + interfaceName);
        pw.println("{");
        pw.indent();
        return pw;
    }

    protected String writeImports(SourceWriter pw)
    {
        final String superClass = AbstractJsonJavaProxy;
        pw.println("import " + superClass + ";");
        pw.println("import java.net.URL;");
        pw.println("import " + HTTPConnector.HttpCallResult.class.getCanonicalName() + ";");
        return superClass;
    }

    private void generateProxyConstructor(@SuppressWarnings("unused") final TreeLogger logger, final SourceWriter w, String interfaceName)
            throws UnableToCompleteException
    {
        final Path relPath = svcInf.getAnnotation(Path.class);
        if (relPath == null || relPath.value() == null)
        {
            throw new UnableToCompleteException("@Path annotation not set for generating " + getProxyQualifiedName());
        }
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

    private void generateProxyMethods(final TreeLogger logger, final SourceWriter srcWriter) throws UnableToCompleteException
    {
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        for (final ExecutableElement m : methods)
        {
            generateProxyMethod(logger, m, srcWriter);
        }
    }

    protected String[] writeSerializers(SourceWriter w, List<? extends VariableElement> params, TypeMirror resultType)
    {
        return new String[1];
    }

    protected void generateProxyMethod(@SuppressWarnings("unused") final TreeLogger logger, final ExecutableElement method, final SourceWriter w)
            throws UnableToCompleteException
    {
        w.println();
        final List<? extends VariableElement> params = method.getParameters();
        final TypeMirror callback = method.getReturnType();// params[params.length - 1];
        TypeMirror resultType = callback;
        //    final JClassType resultType =
        //        callback.isParameterized().getTypeArgs()[0];
        final String[] serializerFields = writeSerializers(w, params, resultType);
        String resultField = serializerFields[serializerFields.length - 1];
        resultField = resultField != null ? resultField : "";

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

            //nameFactory.addName(pname);
            w.print("p"+i);
            argsBuilder.append("p"+i);
        }

        StringBuilder exceptions = new StringBuilder();
        final List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        for (TypeMirror type : thrownTypes)
        {
            if (exceptions.length() > 0)
            {
                exceptions.append(",");
            }
            else
            {
                exceptions.append(" throws ");
            }
            exceptions.append(type.toString());
        }
        w.println(") " + exceptions.toString() + " {");
        w.indent();
        String containerClass = "null";
        final String resultClassname;
        final String cast;
        {
            if ((resultType instanceof DeclaredType) && ((DeclaredType) resultType).getTypeArguments() != null && !((DeclaredType) resultType)
                    .getTypeArguments().isEmpty())
            {
                final DeclaredType resultType1 = (DeclaredType) resultType;
                //final List<? extends TypeMirror> typeArguments = resultType1.getTypeArguments();
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
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final DeclaredType CollectionType;
        {
            final Elements elementUtils = processingEnvironment.getElementUtils();
            CollectionType = typeUtils.getDeclaredType(elementUtils.getTypeElement(Collection.class.getCanonicalName()));
        }
        String postParamName = null;
        w.println("StringBuilder postBody = new StringBuilder();");

        for (int i = 0; i < params.size(); i++)
        {

            final VariableElement param = params.get(i);
            final TypeMirror paramType = param.asType();
            String pname = "p"+i;
            final QueryParam queryAnnotation = param.getAnnotation(QueryParam.class);
            final PathParam pathAnnotation = param.getAnnotation(PathParam.class);
            final HeaderParam headerAnnotation = param.getAnnotation(HeaderParam.class);

            if (queryAnnotation != null || pathAnnotation != null || headerAnnotation != null)
            {
                w.println("if (" + pname + " != null) {");
                w.indent();
                w.println("final StringBuilder param= new StringBuilder();");
                final boolean boxedCharacter = SerializerCreator.isBoxedCharacter(paramType);
                final boolean jsonPrimitive = SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType);
                final boolean jsonString = SerializerCreator.isJsonString(paramType);
                if (boxedCharacter)
                {
                    w.println( "param.append(" + pname + ".toString());");
                }
                else
                {
                    if ((jsonPrimitive))
                    {
                        if (jsonString)
                        {
                            w.println("param.append(" + pname + ");");
                            //w.print( pname );
                        }
                        else
                        {
                            w.println("param.append(" + pname + " + \"\");");
//                            w.print(  pname + "+\"\"");
                        }
                    }
                    //                    else if ((headerAnnotation != null || queryAnnotation!= null ) &&(SerializerCreator.isArray(paramType) || typeUtils.isAssignable(paramType, CollectionType)))
                    //                    {
                    //                        if ( pathAnnotation != null)
                    //                        {
                    //                            throw new UnableToCompleteException("Path annotation is not allowed for non primitive arguments " + className + "." + methodName + "." + pname);
                    //                        }
                    //                        // TODO list of complex objects
                    //                        w.println("{");
                    //                        w.indent();
                    //                        w.println("boolean first = true;");
                    //                        w.println(StringBuilder.class.getCanonicalName() + " sb = new " + StringBuilder.class.getCanonicalName() + "();");
                    //                        w.println("for(" + paramType.toString() + " listParam:" + pname + ") {");
                    //
                    //                        w.println("if(first) {");
                    //                        w.indent();
                    //                        w.println("first = false;");
                    //                        w.outdent();
                    //                        w.println("} else {");
                    //                        String name = queryAnnotation != null ? queryAnnotation.value() : headerAnnotation.value() ;
                    //                        w.println("sb.append(\"&" + name + "=\");");
                    //                        w.println("}");
                    //                        w.indent();
                    //                        //asd&selection=peta&selection=qwe
                    //                        //?selection=asd&selection=peta&selection=qwe
                    //                        //        ?selection=
                    //                        w.outdent();
                    //                        w.println("} ");
                    //                        w.indent();
                    //                        TypeMirror listParamType = null;
                    //                        final String encodedParam = writeEncoder(listParamType,"listParam");
                    //                        w.println("sb.append(" + encodedParam +");");
                    //                        w.outdent();
                    //                        w.println("}");
                    //                        w.outdent();
                    //                        w.println("param" + i + " = sb.toString();");
                    //                        w.println("}");
                    //                    }
                    else
                    {
                        writeEncoded(w,paramType, pname, serializerFields[i]);
                    }
                }
                w.println(" ");
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
                    w.println(queryAnnotation.value() + "=\"+param" + ".toString();");
                }
                if (headerAnnotation != null)
                {
                    w.println("additionalHeaders.put(\"" + headerAnnotation.value() + "\", param.toString());");
                }
                if (pathAnnotation != null)
                {

                    final String pathVariable = "\\\\{" + pathAnnotation.value() + "\\\\}";
                    w.println("subPath = subPath.replaceFirst(\"" + pathVariable + "\", param.toString());");
                }
                w.outdent();
                w.println("}");
            }
            else
            {
                if (method.getAnnotation(GET.class) != null)
                {
                    throw new UnableToCompleteException("param type " + pname + " doesnt have a param annotation in " + className + "." + methodName
                            + " Please use QueryParam, PathParam or HeaderParam");
                }
                if (postParamName != null)
                {
                    throw new UnableToCompleteException(" post param already set in " + className + "." + methodName + " " + postParamName + "," + pname
                            + " only one post param is allowed.");
                }
                writeBody(w,paramType, pname,serializerFields[i]);
                postParamName = pname;
            }
        }


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
        else if (method.getAnnotation(GET.class) != null)
        {
            methodType = "GET";
        }
        else
        {
            throw new UnableToCompleteException("No accessor annotation on pubic method " + className + "." + methodName);
        }
        writeCall(w, resultType, resultField, containerClass, resultClassname, className, methodType);
        if (hasReturn)
        {
            w.println("return (" + cast + ") result;");
        }

        w.outdent();
        w.println("} catch (Exception ex) {");
        w.indent();
        for (TypeMirror exception : thrownTypes)
        {
            String exName = exception.toString();
            w.println("if ( ex instanceof " + exName + " ) throw (" + exName + ") ex;");
        }
        w.println("throw new RuntimeException(ex);");
        w.outdent();
        w.println("}");
        w.outdent();
        w.println("}");

    }

    protected void writeCall(SourceWriter w, TypeMirror resultType, String resultField, String containerClass, String resultClassname, String className,
            String methodType)
    {
        w.println("URL methodURL = getMethodUrl(\"" + className + "\", subPath);");
        w.println("HttpCallResult resultMessage = sendCallWithString_(\"" + methodType
                + "\", methodURL, postBody.toString(), customConnector.getAccessToken(),additionalHeaders);");
        w.println("Class resultType = " + resultClassname + ".class;");
        w.println("Class containerClass = " + containerClass + ";");
        w.println("final Object result = getResult(resultMessage, resultType, containerClass);");
    }

    protected void writeBody(SourceWriter w,TypeMirror paramType, String pname, String serializerField)
    {
        w.println( "postBody.append(serializeArgument(" + pname + "));");
        //return  pname + " != null ? " + pname + ".toString() : \"\"";
    }

    protected void writeEncoded(SourceWriter writer,TypeMirror paramType, String pname, String serializedField)
    {
        writer.print("param.append(serializeArgumentUrl(" + pname + "));");
        //return  pname + " != null ? " + pname + ".toString() : \"\"";
    }

    private String getProxyQualifiedName()
    {
        final String[] name = synthesizeTopLevelClassName(svcInf, getProxySuffix(), nameFactory, processingEnvironment);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    public abstract String getProxySuffix();

    protected String getProxySimpleName()
    {
        return synthesizeTopLevelClassName(svcInf, getProxySuffix(), nameFactory, processingEnvironment)[1];
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
