package org.rapla.rest.generator.internal;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.rapla.inject.InjectionContext;
import org.rapla.inject.generator.internal.SourceWriter;

public class GwtProxyCreator extends AbstractClientProxyCreator implements SerializerClasses
{
    public static final String PROXY_SUFFIX = "_GwtJsonProxy";

    public GwtProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, SerializerCreator serializerCreator, ResultDeserializerCreator deserializerCreator, String generatorName)
    {
        super(remoteService, processingEnvironment, serializerCreator, deserializerCreator, generatorName, InjectionContext.gwt, "GwtClientServerConnector");
    }

    @Override protected String encode(String encodedParam)
    {
        return "GwtClientServerConnector.encodeBase64(" + encodedParam + ")";
    }

    @Override public String getProxySuffix()
    {
        return PROXY_SUFFIX;
    }

    @Override protected void writeImports(SourceWriter pw)
    {
        pw.println("import " + GwtClientServerConnector + ";");
    }

    protected void serializeArg2(SourceWriter w, String targetName, String serializerField, String pName, TypeMirror paramType, boolean encode)
    {
        w.println("final StringBuilder innerParamSb = new StringBuilder();");
        if (serializerCreator.needsTypeParameter(paramType, processingEnvironment))
        {
            w.print(serializerField);
        }
        else
        {
            serializerCreator.generateSerializerReference(paramType, w, false);
        }
        if (isDate( paramType))
        {
            w.println(".serializeDate(innerParamSb, " + pName + ");");
        }
        else
        {
            w.println(".printJson(innerParamSb, " + pName + ");");
        }
        if (encode)
        {
            w.println(targetName + ".append(" + encode("innerParamSb.toString()") + ");");
        }
        else
        {
            w.println(targetName + ".append(innerParamSb.toString());");
        }
    }

    protected String[] writeSerializers(SourceWriter w, List<? extends VariableElement> params, TypeMirror resultType)
    {
        final String[] serializerFields = new String[params.size() + 1];
        for (int i = 0; i < params.size() /*- 1*/; i++)
        {
            final VariableElement variableElement = params.get(i);
            TypeMirror pType = variableElement.asType();
            if (isQueryOrHeaderParam(pType) && (isSetOrList( pType) || isArray(pType)))
            {
                pType = ((DeclaredType) pType).getTypeArguments().get(0);
            }
            if (serializerCreator.needsTypeParameter(pType, processingEnvironment))
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
        String fieldName = "serializer_" + instanceField++;
        serializerFields[serializerFields.length - 1] = fieldName;
        w.print("private static final ");
        w.print(ResultDeserializer);
        w.print(" ");
        w.print(fieldName);
        w.print(" = ");
        if (resultType.getKind() == TypeKind.VOID || resultType.toString().equals("java.lang.Void"))
        {
            w.print("null");
        }
        else
        {
            deserializerCreator.generateDeserializerReference(resultType, w);
        }
        w.println(";");
        return serializerFields;
    }

}
