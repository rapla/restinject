package org.rapla.rest.generator.internal;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.generator.internal.SourceWriter;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.ResolvedPromise;

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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.*;

public abstract class AbstractClientProxyCreator
{
    protected final ProcessingEnvironment processingEnvironment;
    protected TypeElement svcInf;
    protected String generatorName;
    protected int instanceField;
    private final InjectionContext context;

    public static final String AbstractJsonProxy = "org.rapla.rest.client.AbstractJsonProxy";
    private String connectorName;
    private SerializeCheck serializerCreator;

    protected AbstractClientProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName, InjectionContext context, String connectorName)
    {
        this.context = context;
        this.processingEnvironment = processingEnvironment;
        this.generatorName = generatorName;
        svcInf = remoteService;
        this.serializerCreator = new SerializeCheck(processingEnvironment,generatorName);
        this.connectorName = connectorName;
    }

    abstract protected String encode(String encodingParam);

    protected void writeParam(SourceWriter w, String targetName, TypeMirror paramType, String pName, String serializerField, String annotationKey)
    {
        if (annotationKey != null && isSetOrListOrArray(paramType))
        {
            final TypeMirror typeMirror = isArray(paramType) ?
                    ((ArrayType) paramType).getComponentType() :
                    ((DeclaredType) paramType).getTypeArguments().get(0);
            w.println("if (" + pName + " != null) {");
            w.indent();
            w.println("for(" + typeMirror.toString() + " innerParam : " + pName + ") {");
            w.indent();
            w.println("if(" + targetName + ".length() > 0) " + targetName + ".append(\"&" + annotationKey + "=\");");
            final boolean primitive = SerializeCheck.isPrimitive(typeMirror);
            if (!primitive)
            {
                w.println("if(innerParam != null) {");
                w.indent();
            }
            serializeArg(w, targetName, serializerField, "innerParam", typeMirror, true, false);
            if (!primitive)
            {
                w.outdent();
                w.println("}");
            }
            w.outdent();
            w.println("}");
            w.outdent();
            w.println("}");
        }
        else
        {
            final boolean primitive = SerializeCheck.isPrimitive(paramType);
            if (!primitive)
            {
                w.println("if (" + pName + " != null) {");
                w.indent();
            }
            serializeArg(w, targetName, serializerField, pName, paramType, false, annotationKey == null);
            if (!primitive)
            {
                w.outdent();
                w.println("}");
            }

        }
    }

    private void serializeArg(SourceWriter w, String targetName, String serializerField, String pName, TypeMirror paramType, boolean encode, boolean forceJson)
    {
        if (((SerializeCheck.isJsonPrimitive(paramType) || SerializeCheck.isBoxedPrimitive(paramType))) && !(forceJson && SerializeCheck
                .isJsonString(paramType)))
        {
            if (SerializeCheck.isJsonString(paramType) && encode)
            {
                w.println(targetName + ".append(" + encode(pName) + ");");
            }
            else if ((SerializeCheck.isBoxedCharacter(paramType) || TypeKind.CHAR == paramType.getKind()) && encode)
            {
                w.println(targetName + ".append(" + encode("\"\"+" + pName) + ");");
            }
            else
            {
                w.println(targetName + ".append(" + pName + ");");
            }
        }
        else
        {
            serializeArg2(w, targetName, serializerField, pName, paramType, encode);
        }
    }

    protected boolean isDate(TypeMirror paramType)
    {
        final DeclaredType declaredType = getDeclaredType(Date.class);
        return processingEnvironment.getTypeUtils().isAssignable(declaredType, paramType);
    }

    abstract protected void serializeArg2(SourceWriter w, String targetName, String serializerField, String pName, TypeMirror paramType, boolean encode);

    abstract protected String[] writeSerializers(SourceWriter w, List<? extends VariableElement> params, TypeMirror resultType);

    abstract protected String getProxySuffix();

    protected String getGeneratorString(String interfaceName)
    {
        String comments = "annotation in " + interfaceName;
        return "@javax.annotation.Generated(value=\"" + generatorName + "\", comments=\"" + comments + "\")";
    }

    public String create() throws UnableToCompleteException, IOException
    {
        String interfaceName = getInterfaceName();
        checkMethods( interfaceName, processingEnvironment);
        final SourceWriter srcWriter = getSourceWriter(interfaceName);
        if (srcWriter == null)
        {
            return getProxyQualifiedName();
        }

        generateProxyConstructor( srcWriter, interfaceName);
        generateProxyMethods( srcWriter);
        srcWriter.outdent();
        srcWriter.println("};");
        srcWriter.close();

        return getProxyQualifiedName();
    }

    protected String getInterfaceName() {
        TypeElement erasedType = SerializeCheck.getErasedType(svcInf, processingEnvironment);
        String interfaceName = SerializeCheck.erasedTypeString(erasedType);
        return interfaceName;
    }

    private void checkMethods(String interfaceName, final ProcessingEnvironment processingEnvironment) throws UnableToCompleteException
    {

        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        for (final ExecutableElement m : methods)
        {
            try
            {
                final List<? extends VariableElement> params = m.getParameters();

                for (VariableElement p : params)
                {
                    //logger.branch(TreeLogger.DEBUG, m.getName() + ", parameter " + p.getName());
                    //final TypeElement typeP = (TypeElement) processingEnvironment.getTypeUtils().asElement(p.asType());
                    TypeMirror typeP = p.asType();
                    serializerCreator.checkCanSerialize(typeP);
                }
                TypeMirror returnType = m.getReturnType();
                if (SerializeCheck.isPrimitive(returnType))
                {
                    continue;
                }
                //.branch(TreeLogger.DEBUG, m.getName() + ", result " + resultType.getQualifiedSourceName());
                final boolean isPromise = isPromise(returnType);
                if (isPromise)
                {
                    returnType = getPromiseTypeArgument(m, returnType);
                }
                if ( SerializeCheck.isVoid(returnType))
                {
                    continue;
                }
                serializerCreator.checkCanSerialize(returnType);
                if (SerializeCheck.isArray(returnType))
                {
                    // Arrays need a special deserializer
                }
                else if (!SerializeCheck.isPrimitive(returnType) && !SerializeCheck.isBoxedPrimitive(returnType) && returnType.getKind() != TypeKind.VOID)
                {
                    // Non primitives get deserialized by their normal serializer
                }
                // (Boxed)Primitives are left, they are handled specially
            }
            catch ( UnableToCompleteException ex)
            {
                throw new UnableToCompleteException("Can't generate method " + interfaceName + "." + m.getSimpleName().toString() + " cause " + ex.getMessage(),
                        ex);
            }
        }
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

    protected SourceWriter getSourceWriter(String interfaceName)
    {
        final String pkgName = processingEnvironment.getElementUtils().getPackageOf(svcInf).getQualifiedName().toString();
        final String className = svcInf.getSimpleName().toString() + getProxySuffix();
        SourceWriter pw = new SourceWriter(pkgName, className, processingEnvironment);
        pw.println("package " + pkgName + ";");
        pw.println("import " + Map.class.getCanonicalName() + ";");
        pw.println("import " + ResolvedPromise.class.getCanonicalName() + ";");
        pw.println("import " + HashMap.class.getCanonicalName() + ";");
        pw.println("import " + CustomConnector.class.getCanonicalName() + ";");
        pw.println("import " + DefaultImplementation.class.getCanonicalName() + ";");
        pw.println("import " + InjectionContext.class.getCanonicalName() + ";");
        writeImports(pw);
        pw.println();
        pw.println(getGeneratorString(interfaceName));
        pw.println(
                "@" + DefaultImplementation.class.getSimpleName() + "(of = " + interfaceName + ".class, context=" + InjectionContext.class.getSimpleName() + "."
                        + context + ")");
        pw.println("public class " + className + " extends " + AbstractJsonProxy + " implements " + interfaceName);
        pw.println("{");
        pw.indent();
        return pw;
    }

    abstract protected void writeImports(SourceWriter pw);

    private void generateProxyConstructor(@SuppressWarnings("unused") final SourceWriter w, String interfaceName)
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
            String path = relPath.value();
            w.println("setPath(\"" + path + "\");");
            w.outdent();
            w.println("}");
        }

    }

    private void generateProxyMethods( final SourceWriter srcWriter) throws UnableToCompleteException
    {
        final List<ExecutableElement> methods = getMethods(processingEnvironment);
        for (final ExecutableElement m : methods)
        {
            generateProxyMethod( m, srcWriter);
        }
    }

    protected boolean isQueryOrHeaderParam(TypeMirror paramType)
    {
        return paramType.getAnnotation(QueryParam.class) != null || paramType.getAnnotation(HeaderParam.class) != null;
    }

    protected boolean isArray(TypeMirror paramType)
    {
        return paramType.getKind() == TypeKind.ARRAY;
    }

    protected boolean isSetOrListOrArray(TypeMirror paramType)
    {
        return isArray(paramType) || isSetOrList(paramType);
    }

    protected boolean isSetOrList(TypeMirror paramType)
    {
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final DeclaredType ListType = getDeclaredType(List.class);
        final DeclaredType SetType = getDeclaredType(Set.class);
        final boolean isList = typeUtils.isAssignable(paramType, ListType);
        final boolean isSet = typeUtils.isAssignable(paramType, SetType);
        return isList || isSet;
    }

    protected boolean isPromise(TypeMirror paramType)
    {
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final DeclaredType PromiseType = getDeclaredType(Promise.class);
        final boolean isPromise = typeUtils.isAssignable(paramType, PromiseType);
        return isPromise;
    }

    protected void generateProxyMethod(@SuppressWarnings("unused")  final ExecutableElement method, final SourceWriter w)
            throws UnableToCompleteException
    {
        w.println();
        final List<? extends VariableElement> params = method.getParameters();

        final TypeMirror returnType = method.getReturnType();
        TypeMirror resultType;
        final boolean isPromise = isPromise(returnType);
        if (isPromise)
        {
            resultType = getPromiseTypeArgument(method, returnType);
        }
        else
        {
            resultType = returnType;
        }
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
            w.print(param.asType().toString());
            w.print(" ");
            w.print("p" + i);
            argsBuilder.append("p" + i);
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
        final String cast = returnType.toString();

        final boolean hasReturn = !cast.toLowerCase().equals("void");

        w.println("try{");
        w.indent();
        w.println("java.lang.String subPath = \"" + (method.getAnnotation(Path.class) != null ? method.getAnnotation(Path.class).value() : "") + "\";");
        w.println("final Map<java.lang.String, java.lang.String>additionalHeaders = new HashMap<>();");
        final String className = svcInf.getQualifiedName().toString();
        boolean firstQueryParam = true;

        final DeclaredType RuntimeExeption = getDeclaredType(RuntimeException.class);
        String postParamName = null;
        w.println("StringBuilder postBody = new StringBuilder();");
        final Types typeUtils = processingEnvironment.getTypeUtils();

        for (int i = 0; i < params.size(); i++)
        {
            final VariableElement param = params.get(i);
            final TypeMirror paramType = param.asType();
            String pname = "p" + i;
            final QueryParam queryAnnotation = param.getAnnotation(QueryParam.class);
            final PathParam pathAnnotation = param.getAnnotation(PathParam.class);
            final HeaderParam headerAnnotation = param.getAnnotation(HeaderParam.class);

            if (queryAnnotation != null || pathAnnotation != null || headerAnnotation != null)
            {
                final String annotationKey = queryAnnotation != null ? queryAnnotation.value() : headerAnnotation != null ? headerAnnotation.value() : null;
                final boolean boxedCharacter = SerializeCheck.isBoxedCharacter(paramType);
                final boolean boxedPrimitive = SerializeCheck.isBoxedPrimitive(paramType);
                final boolean jsonPrimitive = SerializeCheck.isJsonPrimitive(paramType) || boxedPrimitive;
                final boolean jsonString = SerializeCheck.isJsonString(paramType);
                if (jsonPrimitive && !jsonString)
                {
                    w.println("{");
                }
                else
                {
                    w.println("if (" + pname + " != null) {");
                }
                w.indent();
                w.println("final StringBuilder param= new StringBuilder();");
                writeParam(w, "param", paramType, pname, serializerFields[i], annotationKey);
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
                    if (isSetOrListOrArray(paramType))
                    {
                        w.println(queryAnnotation.value() + "=\"+param.toString();");
                    }
                    else
                    {
                        w.println(queryAnnotation.value() + "=\"+" + encode("param.toString()") + ";");
                    }
                }
                if (headerAnnotation != null)
                {
                    if (isSetOrListOrArray(paramType))
                    {
                        w.println("additionalHeaders.put(\"" + headerAnnotation.value() + "\", param.toString());");
                    }
                    else
                    {
                        w.println("additionalHeaders.put(\"" + headerAnnotation.value() + "\", " + encode("param.toString()") + ");");
                    }
                }
                if (pathAnnotation != null)
                {

                    final String pathVariable = "\\\\{" + pathAnnotation.value() + "\\\\}";
                    w.println("subPath = subPath.replaceFirst(\"" + pathVariable + "\", " + encode("param.toString()") + ");");
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
                writeParam(w, "postBody", paramType, pname, serializerFields[i], null);
                postParamName = pname;
            }
        }

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
        else if (method.getAnnotation(OPTIONS.class) != null)
        {
            methodType = "OPTIONS";
        }
        else
        {
            throw new UnableToCompleteException("No accessor annotation on pubic method " + className + "." + methodName);
        }
        w.println("String methodUrl = getMethodUrl( subPath);");
        w.print("Object result = ");
        w.print(connectorName);
        w.print(".doInvoke(");
        w.print("\"" + methodType + "\"");
        w.print(", methodUrl , additionalHeaders,postBody.toString(),");
        w.print(resultField);
        w.print(", \"" + resultType.toString() + "\"");
        w.print(", connector");
        w.print(", " + (isPromise ? true : false));
        w.print(");");
        w.println(" ");

        if (hasReturn)
        {
            w.println("return (" + cast + ") result;");
        }
        w.outdent();
        w.println("} catch (Exception ex) {");
        w.indent();
        w.println("if ( ex instanceof RuntimeException ) throw (RuntimeException) ex;");
        for (TypeMirror exception : thrownTypes)
        {
            if (typeUtils.isAssignable(exception, RuntimeExeption))
            {
                continue;
            }
            String exName = exception.toString();
            w.println("if ( ex instanceof " + exName + " ) throw (" + exName + ") ex;");
        }
        w.println("throw new RuntimeException(ex);");
        w.outdent();
        w.println("}");

        w.outdent();
        w.println("}");

    }

    private TypeMirror getPromiseTypeArgument(ExecutableElement method, TypeMirror returnType) throws UnableToCompleteException
    {
        TypeMirror resultType;
        boolean isGeneric = SerializeCheck.isParameterized(returnType);
        if (isGeneric)
        {
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) returnType).getTypeArguments();
            if (typeArguments.size() != 1)
            {
                throw new UnableToCompleteException(
                        "Can't generate Proxy in  " + method.toString() + ". Promise needs one type argument. Use typed promises like Promise<String>");
            }
            else
            {
                resultType = typeArguments.get(0);
            }
        }
        else
        {
            throw new UnableToCompleteException("Can't generate Proxy in  " + method.toString()
                    + ". Promise is untyped or has more then one type argument. Use typed promises like Promise<String>");
        }
        return resultType;
    }

    protected DeclaredType getDeclaredType(Class<?> clazz)
    {
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final Elements elementUtils = processingEnvironment.getElementUtils();
        final TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
        return typeUtils.getDeclaredType(typeElement);
    }

    protected String getProxyQualifiedName()
    {
        final String[] name = synthesizeTopLevelClassName(svcInf, getProxySuffix(), processingEnvironment);
        return name[0].length() == 0 ? name[1] : name[0] + "." + name[1];
    }

    protected String getProxySimpleName()
    {
        return synthesizeTopLevelClassName(svcInf, getProxySuffix(), processingEnvironment)[1];
    }

    static String[] synthesizeTopLevelClassName(TypeElement type, String suffix, ProcessingEnvironment processingEnvironment)
    {
        // Gets the basic name of the type. If it's a nested type, the type name
        // will contains dots.
        //
        String className;
        String packageName;

        TypeMirror typeMirror = getLeafType(type.asType());
        TypeElement leafType = (TypeElement) processingEnvironment.getTypeUtils().asElement(typeMirror);
        if (SerializeCheck.isPrimitive(typeMirror))
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

        boolean isGeneric = SerializeCheck.isParameterized(typeMirror);
        if (isGeneric)
        {
            final List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
            for (TypeMirror param : typeArguments)
            {
                className += "_";
                className += param.toString().replace('.', '_');
            }
        }

        boolean isArray = SerializeCheck.isArray(typeMirror);
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
