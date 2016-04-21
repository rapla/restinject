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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.google.gwt.dev.javac.testing.Source;
import org.rapla.inject.generator.internal.SourceWriter;

import com.google.gwt.http.client.RequestBuilder;
import org.rapla.rest.client.CustomConnector;

public class GwtProxyCreator extends AbstractClientProxyCreator
{
    public static final String PROXY_SUFFIX = "_GwtJsonProxy";
    private int instanceField;

    public GwtProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        super(remoteService,processingEnvironment, generatorName);
    }

    @Override
    public String getProxySuffix()
    {
        return PROXY_SUFFIX;
    }


    @Override
    protected String writeImports(SourceWriter pw)
    {
        final String superClass = AbstractJsonProxy;
        pw.println("import " + superClass + ";");
        pw.println("import " + JsonSerializer + ";");
        pw.println("import com.google.gwt.core.client.JavaScriptObject;");
        pw.println("import " + ResultDeserializer + ";");
        pw.println("import com.google.gwt.core.client.GWT;");
        pw.println("import " + RequestBuilder.class.getCanonicalName() + ";");
        return superClass;
    }

    @Override protected void writeBody(SourceWriter w,TypeMirror paramType, String pName,String serializerField)
    {

        if (SerializerCreator.isBoxedCharacter(paramType))
        {
            w.println("postBody.append(\"\\\"\") + " + JsonSerializer_simple + ".escapeChar(" + pName + "))+ \\\"\"));");
        }
        else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType)) && !SerializerCreator.isJsonString(paramType))
        {
            w.println("postBody.append(" + pName  + ");");
        }
        else
        {
            w.println("if (" + pName + " != null) {");
            w.indent();
            if (SerializerCreator.needsTypeParameter(paramType, processingEnvironment))
            {
                w.print(serializerField);
            }
            else
            {
                serializerCreator.generateSerializerReference(paramType, w, false);
            }
            w.println(".printJson(postBody, " + pName + ");");
            w.outdent();
            w.println("}");// else {");
            //                    w.indent();
            //                    w.println(reqData + ".append(" + JsonSerializer + ".JS_NULL);");
            //                    w.outdent();
            //                    w.println("}");
        }
    }

    @Override protected void writeEncoded(SourceWriter w,TypeMirror paramType, String pName, String serializerField)
    {
        if (SerializerCreator.isBoxedCharacter(paramType))
        {
            w.print("param.append(" +JsonSerializer_simple + ".escapeChar(" + pName + ")));");
        }
        else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType)) && !SerializerCreator.isJsonString(paramType))
        {
            w.print("param.append(" + pName + ");" );
        }
        else
        {
            final boolean needsTypeParameter = SerializerCreator.needsTypeParameter(paramType, processingEnvironment);
            if ( needsTypeParameter )
            {
                w.print(serializerField + ".printJson(param," + pName + ");");
            }
            else
            {
                serializerCreator.generateSerializerReference(paramType, w, false);
            }
        }

    }

    /*
    @Override
    protected void generateProxyMethod(@SuppressWarnings("unused") final TreeLogger logger, final ExecutableElement method, final SourceWriter w)
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
            //final TypeElement paramType = (TypeElement) processingEnvironment.getTypeUtils().asElement(param.asType());
            w.print(param.asType().toString());
            w.print(" ");

            nameFactory.addName(pname);
            w.print(pname);
        }
        final List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
        if (thrownTypes.isEmpty())
        {
            w.println(") {");
        }
        else
        {
            w.print(") throws ");
            boolean first = true;
            for (TypeMirror typeMirror : thrownTypes)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    w.print(", ");
                }
                w.print(typeMirror.toString());
            }
            w.println("{");
        }
        w.indent();
        w.println("final java.util.Map<String, String> additionalHeaders = new java.util.HashMap<String, String>();");

        final String reqDataStr;
        final String subPath;
        final Path annotation = method.getAnnotation(Path.class);
        if (annotation != null)
        {
            subPath = annotation.value();
        }
        else
        {
            subPath = "";
        }
        w.println("String subPath = \"" + subPath + "\";");
        if (params.isEmpty())
        {
            reqDataStr = "\"[]\"";
        }
        else
        {
            boolean queryParamAdded = false;
            final String reqData = nameFactory.createName("reqData");
            w.println("final StringBuilder " + reqData + " = new StringBuilder();");
            needsComma = false;
            for (int i = 0; i < params.size(); i++)
            {
                final VariableElement param = params.get(i);
                final String pName = param.getSimpleName().toString();
                final TypeMirror paramType = param.asType();

                final PathParam pathParamAnnotation = param.getAnnotation(PathParam.class);
                if (pathParamAnnotation != null)
                {
                    final String value = pathParamAnnotation.value();
                    final String expInPath = "{" + value + "}";
                    final String convertedValue = convertParam(paramType, pName, serializerFields, i);
                    w.println("subPath = subPath.replaceFirst(\"" + expInPath + "\", " + convertedValue + ");");
                    continue;
                }
                final QueryParam queryParamAnnotation = param.getAnnotation(QueryParam.class);
                if (queryParamAnnotation != null)
                {
                    if (queryParamAdded)
                    {
                        w.println("subPath +=\"&" + queryParamAnnotation.value() + "=\"+" + convertParam(paramType, pName, serializerFields, i) + ";");
                    }
                    else
                    {
                        queryParamAdded = true;
                        w.println("subPath +=\"?" + queryParamAnnotation.value() + "=\"+" + convertParam(paramType, pName, serializerFields, i) + ";");
                    }
                    continue;
                }
                final HeaderParam headerParamAnnotation = param.getAnnotation(HeaderParam.class);
                if (headerParamAnnotation != null)
                {
                    w.println("additionalHeaders.put(\"" + headerParamAnnotation.value() + "\", " + convertParam(paramType, pName, serializerFields, i) + ");");
                    continue;
                }
                final FormParam formParamAnnotation = param.getAnnotation(FormParam.class);
                if (formParamAnnotation != null)
                {
                    w.println("additionalHeaders.put(\"" + formParamAnnotation.value() + "\", " + convertParam(paramType, pName, serializerFields, i) + ");");
                    continue;
                }
                if (needsComma)
                {
                    w.println(reqData + ".append(\",\");");
                }
                else
                {
                    needsComma = true;
                }

                //                pType == JPrimitiveType.CHAR ||
                if (SerializerCreator.isBoxedCharacter(paramType))
                {
                    w.println(reqData + ".append(\"\\\"\");");
                    w.println(reqData + ".append(" + JsonSerializer_simple + ".escapeChar(" + pName + "));");
                    w.println(reqData + ".append(\"\\\"\");");
                }
                else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType))
                        && !SerializerCreator.isJsonString(paramType))
                {
                    w.println(reqData + ".append(" + pName + ");");
                }
                else
                {
                    w.println("if (" + pName + " != null) {");
                    w.indent();
                    if (SerializerCreator.needsTypeParameter(paramType, processingEnvironment))
                    {
                        w.print(serializerFields[i]);
                    }
                    else
                    {
                        serializerCreator.generateSerializerReference(paramType, w, false);
                    }
                    w.println(".printJson(" + reqData + ", " + pName + ");");
                    w.outdent();
                    w.println("}");// else {");
//                    w.indent();
//                    w.println(reqData + ".append(" + JsonSerializer + ".JS_NULL);");
//                    w.outdent();
//                    w.println("}");
                }
            }
            reqDataStr = reqData + ".toString()";
        }
        final boolean isVoidReturnType = "void".equals(method.getReturnType().toString());
        w.println("try {");
        w.indent();
        if (!isVoidReturnType)
        {
            w.print("return (" + method.getReturnType().toString() + ") ");
        }
        w.print("doInvoke(");
        final String methodType;
        if (method.getAnnotation(POST.class) != null)
        {
            methodType = "RequestBuilder.POST";
        }
        else if (method.getAnnotation(PUT.class) != null)
        {
            methodType = "RequestBuilder.PUT";
        }
        else if (method.getAnnotation(DELETE.class) != null)
        {
            methodType = "RequestBuilder.DELETE";
        }
        else // assume if(method.getAnnotation(GET.class) != null)
        {
            methodType = "RequestBuilder.GET";
        }

        writeCall(w, resultType, resultField, reqDataStr, methodType);

        w.println(");");
        w.outdent();
        w.println("} catch(Exception e){");
        w.indent();
        for (TypeMirror typeMirror : thrownTypes)
        {
            w.println("if (e instanceof " + typeMirror.toString() + ") {");
            w.indent();
            w.println("throw (" + typeMirror.toString() + ") e;");
            w.outdent();
            w.println("}");
        }
        w.println("throw new RuntimeException(e);");
        w.println("}");
        w.outdent();
        w.outdent();
        w.println("}");
    }
    */

    @Override
    protected void writeCall(SourceWriter w, TypeMirror resultType, String resultField, String containerClass, String resultClassname, String className, String methodType)
    {
        w.print("Object result = doInvoke(");
        w.print("\""+methodType+ "\"");
        w.print(", subPath, postBody.toString(), additionalHeaders,");
        if ((resultType instanceof DeclaredType) && ((DeclaredType) resultType).getTypeArguments() != null
                && !((DeclaredType) resultType).getTypeArguments().isEmpty())
        {
            w.print(resultField);
        }
        else if (resultType.getKind() == TypeKind.VOID)
        {
            w.print("null");
        }
        else
        {
            deserializerCreator.generateDeserializerReference(resultType, w);
        }
        w.print(");");
        w.println(" ");
    }

    protected String[] writeSerializers(SourceWriter w, List<? extends VariableElement> params, TypeMirror resultType)
    {
        final String[] serializerFields = new String[params.size() + 1];
        for (int i = 0; i < params.size() /*- 1*/; i++)
        {
            final VariableElement variableElement = params.get(i);
            final TypeMirror pType = variableElement.asType();
            if (SerializerCreator.needsTypeParameter(pType, processingEnvironment))
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
        //TypeMirror parameterizedResult = null;
        if (SerializerCreator.isParameterized(resultType))
        {
            String fieldName = "serializer_" + instanceField++;
            serializerFields[serializerFields.length - 1] =  fieldName;
            w.print("private static final ");
            w.print(ResultDeserializer);
            w.print(" ");
            w.print(fieldName);
            w.print(" = ");
            //final List<? extends TypeMirror> typeArguments = ((DeclaredType) resultType).getTypeArguments();

            //parameterizedResult = typeArguments.get(0);
            deserializerCreator.generateDeserializerReference(resultType, w);
            w.println(";");
        }
        return serializerFields;
    }

    private String convertParam(final TypeMirror paramType, final String pName, String[] serializerFields, int index)
    {
        //                pType == JPrimitiveType.CHAR ||
        if (SerializerCreator.isBoxedCharacter(paramType))
        {
            return JsonSerializer_simple + ".escapeChar(" + pName + "));";
        }
        else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType)) && !SerializerCreator.isJsonString(paramType))
        {
            return pName + ";";
        }
        else
        {
            //            final boolean needsTypeParameter = SerializerCreator.needsTypeParameter(paramType, processingEnvironment);
            //            return pName + " != null ? " + (needsTypeParameter ? serializerFields[index] : pName) + ": \"\"";
            // FIXME think about serializing arrays and lists
            return pName + " != null ? " + pName + ".toString(): \"\"";
            // serializerCreator.generateSerializerReference(paramType, w, false);
        }
    }


}
