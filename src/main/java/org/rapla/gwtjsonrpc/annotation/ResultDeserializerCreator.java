// Copyright 2009 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.rapla.gwtjsonrpc.rebind.*;
import org.rapla.gwtjsonrpc.rebind.SerializerClasses;

import javax.lang.model.element.TypeElement;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Creator of ResultDeserializers. Actually, only object arrays have created
 * deserializers:
 * All object arrays that have a JSONSerializer for the array component can be
 * generated, but they will need to live in the same package as the serializer.
 * To do this, if the serializer lives in the
 * <code>com.google.gwtjsonrpc.client</code> package (where custom object
 * serializers live), the ResultDeserializer for it's array will be placed in
 * this package as well. Else it will be placed with the serializer in the
 * package the object lives.
 */
class ResultDeserializerCreator {
  private static final String DSER_SUFFIX = "_ResultDeserializer";

  private GeneratorContext context;
  private HashMap<String, String> generatedDeserializers;
  private SerializerCreator serializerCreator;

  private TypeElement targetType;
  private TypeElement componentType;

  ResultDeserializerCreator(GeneratorContext c, SerializerCreator sc) {
    context = c;
    generatedDeserializers = new HashMap<String, String>();
    serializerCreator = sc;
  }

  void create(TreeLogger logger, TypeElement targetType) {
    this.targetType = targetType;
    this.componentType = targetType.getComponentType();

    if (SerializerCreator.isPrimitive(componentType)
        || SerializerCreator.isBoxedPrimitive(componentType)) {
      logger.log(TreeLogger.DEBUG,
          "No need to create array deserializer for primitive array "
              + targetType);
      return;
    }

    if (deserializerFor(targetType) != null) {
      return;
    }

    logger.log(TreeLogger.DEBUG, "Creating result deserializer for "
        + targetType.getSimpleName());
    final PrintWriter srcWriter = getSourceWriter(logger, context);
    if (srcWriter == null) {
      return;
    }
    final String dsn = getDeserializerQualifiedName(targetType);
    generatedDeserializers.put(targetType.getQualifiedName().toString(), dsn);

    generateSingleton(srcWriter);
    generateInstanceMembers(srcWriter);
    generateFromResult(srcWriter);

    srcWriter.commit(logger);
  }

  private void generateSingleton(final PrintWriter w) {
    w.print("public static final ");
    w.print(getDeserializerSimpleName(targetType));
    w.print(" INSTANCE = new ");
    w.print(getDeserializerSimpleName(targetType));
    w.println("();");
    w.println();
  }

  private void generateInstanceMembers(PrintWriter w) {
    w.print("private final ");
    w.print(serializerCreator.serializerFor(targetType));
    w.print(" ");
    w.print("serializer");
    w.print(" = ");
    serializerCreator.generateSerializerReference(targetType, w, true);
    w.println(";");
    w.println();
  }

  private void generateFromResult(PrintWriter w) {
    final String ctn = componentType.getQualifiedName().toString();

    w.println("@Override");
    w.print("public " + ctn + "[] ");
    w.println("fromResult(JavaScriptObject responseObject) {");

    w.print("final " + ctn + "[] tmp = new " + ctn);
    w.println("[getResultSize(responseObject)];");

    w.println("serializer.fromJson(getResult(responseObject), tmp);");
    w.println("return tmp;");

    w.println("}");
  }

  private String getDeserializerQualifiedName(TypeElement targetType) {
    final String pkgName = getDeserializerPackageName(targetType);
    final String className = getDeserializerSimpleName(targetType);
    return pkgName.length() == 0 ? className : pkgName + "." + className;
  }

  private String getDeserializerPackageName(TypeElement targetType) {
    // Place array deserializer in same package as the component deserializer
    final String compSerializer =
        serializerCreator.serializerFor(targetType.getComponentType());
    final int end = compSerializer.lastIndexOf('.');
    return end >= 0 ? compSerializer.substring(0, end) : "";
  }

  private static String getDeserializerSimpleName(TypeElement targetType) {
    return ProxyCreator.synthesizeTopLevelClassName(targetType, DSER_SUFFIX)[1];
  }

  private PrintWriter getSourceWriter(TreeLogger logger,
      GeneratorContext context) {
    String pkgName = getDeserializerPackageName(targetType);
    final String simpleName = getDeserializerSimpleName(targetType);
    final PrintWriter pw;
    final ClassSourceFileComposerFactory cf;

    pw = context.tryCreate(logger, pkgName, simpleName);
    if (pw == null) {
      return null;
    }

    cf = new ClassSourceFileComposerFactory(pkgName, simpleName);
    cf.addImport(JavaScriptObject.class.getCanonicalName());
    cf.addImport(org.rapla.gwtjsonrpc.rebind.SerializerClasses.ResultDeserializer);

    cf.setSuperclass(org.rapla.gwtjsonrpc.rebind.SerializerClasses.ArrayResultDeserializer);
    cf.addImplementedInterface(org.rapla.gwtjsonrpc.rebind.SerializerClasses.ResultDeserializer
        + "<" + targetType.getQualifiedSourceName() + ">");

    return cf.createSourceWriter(context, pw);
  }

  private String deserializerFor(TypeElement targetType) {
    final JType componentType = targetType.getComponentType();
    // Custom primitive deserializers
    if (SerializerCreator.isBoxedPrimitive(componentType))
      return org.rapla.gwtjsonrpc.rebind.SerializerClasses.PrimitiveArrayResultDeserializers + "."
          + componentType.getSimpleSourceName().toUpperCase() + "_INSTANCE";
    final String name =
        generatedDeserializers.get(targetType.getQualifiedSourceName());

    return name == null ? null : name + ".INSTANCE";
  }

  public void generateDeserializerReference(TypeElement targetType, SourceWriter w) {
    if (SerializerCreator.isBoxedPrimitive(targetType)) {
      w.print(SerializerClasses.PrimitiveResultDeserializers);
      w.print(".");
      w.print(targetType.getSimpleName().toString().toUpperCase());
      w.print("_INSTANCE");
    } else if (targetType.isArray() != null) {
      w.print(deserializerFor(targetType.isArray()));
    } else {
      serializerCreator.generateSerializerReference(targetType, w, false);
    }
  }
}
