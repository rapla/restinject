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

import com.google.gwt.http.client.RequestBuilder;
import org.rapla.inject.generator.internal.SourceWriter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

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
            w.println("}");
        }
//        else {
//                                w.indent();
//                                w.println(reqData + ".append(" + JsonSerializer + ".JS_NULL);");
//                                w.outdent();
//                                w.println("}");
//        }
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
                w.print(serializerField);
            }
            else
            {
                serializerCreator.generateSerializerReference(paramType, w, false);
            }
            w.print(".printJson(param," + pName + ");");
        }

    }

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

}
