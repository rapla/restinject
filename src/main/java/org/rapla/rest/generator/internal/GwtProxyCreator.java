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

import org.rapla.inject.generator.internal.SourceWriter;
import org.rapla.rest.client.gwt.internal.impl.JsonCall;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;

import java.util.List;

public class GwtProxyCreator extends AbstractClientProxyCreator
{
    public static final String PROXY_SUFFIX = "_GwtJsonProxy";

    public GwtProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        super(remoteService,processingEnvironment, generatorName);
    }


    @Override
    protected String encode(String encodedParam)
    {
        return "JsonCall.encodeBase64(" + encodedParam  + ")";
    }

    @Override
    public String getProxySuffix()
    {
        return PROXY_SUFFIX;
    }


    @Override
    protected void writeImports(SourceWriter pw)
    {
        pw.println("import " + JsonSerializer + ";");
        pw.println("import com.google.gwt.core.client.JavaScriptObject;");
        pw.println("import " + ResultDeserializer + ";");
        pw.println("import com.google.gwt.core.client.GWT;");
        pw.println("import " + RequestBuilder + ";");
        pw.println("import " + JsonCall + ";");
    }

    @Override
    protected void writeParam(SourceWriter w, String targetName, TypeMirror paramType, String pName, String serializerField, String annotationKey)
    {
        if(annotationKey != null && isSetOrList(paramType))
        {
            final TypeMirror typeMirror = ((DeclaredType) paramType).getTypeArguments().get(0);
            w.println("if (" + pName + " != null) {");
            w.indent();
            w.println("for(" + typeMirror.toString() + " innerParam : " + pName + ") {");
            w.indent();
            w.println("if(" + targetName + ".length() > 0) " + targetName + ".append(\"&" + annotationKey + "=\");");
            if (SerializerCreator.isBoxedCharacter(typeMirror))
            {
                w.println(targetName + ".append(\"\\\"\") + " + JsonSerializer_simple + ".escapeChar("+annotationKey+"=innerParam))+ \\\"\"));");
            }
            else if ((SerializerCreator.isJsonPrimitive(typeMirror) || SerializerCreator.isBoxedPrimitive(typeMirror))
                    && !SerializerCreator.isJsonString(typeMirror))
            {
                w.println(targetName + ".append("+annotationKey+"="+encode("innerParam")+");");
            }
            else
            {
                w.println("if (innerParam != null) {");
                w.indent();
                w.println("final StringBuilder innerParamSb = new StringBuilder();");
                if (SerializerCreator.needsTypeParameter(typeMirror, processingEnvironment))
                {
                    w.print(serializerField);
                }
                else
                {
                    serializerCreator.generateSerializerReference(typeMirror, w, false);
                }
                w.println(".printJson(innerParamSb, innerParam);");
                w.println(targetName + ".append("+encode("innerParamSb.toString()")+");");
                w.outdent();
                w.println("}");
            }
            w.outdent();
            w.println("}");
            w.outdent();
            w.println("}");
            
        }
        else if (SerializerCreator.isBoxedCharacter(paramType))
        {
            w.println(targetName+".append(\"\\\"\") + " + JsonSerializer_simple + ".escapeChar(" + pName + "))+ \\\"\"));");
        }
        else if ((SerializerCreator.isJsonPrimitive(paramType) || SerializerCreator.isBoxedPrimitive(paramType)) && !SerializerCreator.isJsonString(paramType))
        {
            w.println(targetName+".append(" + pName  + ");");
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
            w.println(".printJson("+targetName + ", " + pName + ");");
            w.outdent();
            w.println("}");
        }
    }


    @Override
    protected void writeCall(SourceWriter w, TypeMirror resultType, String resultDeserialzerField, String methodType)
    {
        w.print("Object result = JsonCall.doInvoke(");
        w.print("\""+methodType+ "\"");
        w.print(", methodUrl, postBody.toString(), additionalHeaders,");
        if ((resultType instanceof DeclaredType) && ((DeclaredType) resultType).getTypeArguments() != null
                && !((DeclaredType) resultType).getTypeArguments().isEmpty())
        {
            w.print(resultDeserialzerField);
        }
        else if (resultType.getKind() == TypeKind.VOID)
        {
            w.print("null");
        }
        else
        {
            deserializerCreator.generateDeserializerReference(resultType, w);
        }
        w.print(", \"" + resultType.toString() + "\"");
        w.print(", connector);");
        w.println(" ");
    }
    
    private boolean isListSetParam(TypeMirror paramType)
    {
        return paramType.getAnnotation(QueryParam.class) != null || paramType.getAnnotation(HeaderParam.class) != null;
    }

    protected String[] writeSerializers(SourceWriter w, List<? extends VariableElement> params, TypeMirror resultType)
    {
        final String[] serializerFields = new String[params.size() + 1];
        for (int i = 0; i < params.size() /*- 1*/; i++)
        {
            final VariableElement variableElement = params.get(i);
            TypeMirror pType = variableElement.asType();
            if(isListSetParam(pType))
            {
                // Take inner type
                pType = ((DeclaredType)pType).getTypeArguments().get(0);
            }
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
        if (SerializerCreator.isParameterized(resultType))
        {
            String fieldName = "serializer_" + instanceField++;
            serializerFields[serializerFields.length - 1] =  fieldName;
            w.print("private static final ");
            w.print(ResultDeserializer);
            w.print(" ");
            w.print(fieldName);
            w.print(" = ");
            deserializerCreator.generateDeserializerReference(resultType, w);
            w.println(";");
        }
        return serializerFields;
    }

}
