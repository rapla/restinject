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
import org.rapla.rest.client.swing.JavaClientServerConnector;
import org.rapla.rest.client.swing.JsonRemoteConnector;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class JavaClientProxyCreator extends AbstractClientProxyCreator
{
    public static final String PROXY_SUFFIX = "_JavaJsonProxy";

    public JavaClientProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        super(remoteService, processingEnvironment, generatorName);
    }

    @Override
    protected void writeImports(SourceWriter pw)
    {
        pw.println("import " + JavaClientServerConnector.class.getCanonicalName() + ";");
        pw.println("import java.net.URL;");
        pw.println("import java.net.URLEncoder;");
        pw.println("import " + JavaJsonSerializer + ";");
        pw.println("import " + JsonRemoteConnector.CallResult.class.getCanonicalName() + ";");
    }

    @Override protected String encode(String encodingParam)
    {
        return "URLEncoder.encode(" + encodingParam + ",\"UTF-8\")";
    }

    @Override protected String[] writeSerializers(SourceWriter w, List<? extends VariableElement> params, TypeMirror resultType)
    {
        String containerClass = "null";
        final String resultClassname;
        {
            if ((resultType instanceof DeclaredType) && ((DeclaredType) resultType).getTypeArguments() != null && !((DeclaredType) resultType)
                    .getTypeArguments().isEmpty())
            {
                TypeMirror typeMirror = resultType;
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
            }
            else
            {
                resultClassname = resultType.toString();
            }
        }
        String serialzerName = "serializer_" + instanceField++;
        w.println("JavaJsonSerializer " + serialzerName + " = new JavaJsonSerializer(" + resultClassname + ".class, " + containerClass + ");");

        final int argLength = params.size();
        final String[] strings = new String[argLength + 1];
        for (int i = 0; i < strings.length; i++)
        {
            strings[i] = serialzerName;
        }
        return strings;
    }

    @Override public String getProxySuffix()
    {
        return PROXY_SUFFIX;
    }

    @Override
    protected String getServerConnectorClass()
    {
        return JavaClientServerConnector.class.getSimpleName();
    }



    protected void serializeArg2(SourceWriter w, String targetName, String serializerField, String pName, TypeMirror paramType,boolean encode)
    {
        final String methodName = isDate( paramType)? "serializeDate" : "serializeArgument" ;
        if (encode)
            w.println(targetName + ".append(" + encode(serializerField + "." + methodName +"("+ pName+")") + ");");
        else
            w.println(targetName + ".append(" + serializerField + "." + methodName +"("+pName + "));");
    }

}
