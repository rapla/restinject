package org.rapla.dagger;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.rapla.gwtjsonrpc.annotation.ProxyCreator;
import org.rapla.gwtjsonrpc.annotation.SourceWriter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.generator.AnnotationInjectionProcessor;

import dagger.Module;
import dagger.Provides;
import dagger.internal.Factory;

public class DaggerModuleProcessor
{

    private static class Generated
    {
        private final String interfaceName;
        private final String className;

        protected Generated(String interfaceName, String className)
        {
            this.interfaceName = interfaceName;
            this.className = className;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Generated other = (Generated) obj;
            if (className == null)
            {
                if (other.className != null)
                    return false;
            }
            else if (!className.equals(other.className))
                return false;
            if (interfaceName == null)
            {
                if (other.interfaceName != null)
                    return false;
            }
            else if (!interfaceName.equals(other.interfaceName))
                return false;
            return true;
        }

    }

    private static final int SERVER_SOURCE_WRITER = 0;
    private static final int JAVA_CLIENT_SOURCE_WRITER = 1;
    private static final int GWT_SOURCE_WRITER = 2;
    private final Set<Generated>[] alreadyGenerated = new LinkedHashSet[3];
    private final SourceWriter[] sourceWriters = new SourceWriter[3];
    private final ProcessingEnvironment processingEnvironment;

    public DaggerModuleProcessor(ProcessingEnvironment processingEnvironment)
    {
        super();
        this.processingEnvironment = processingEnvironment;
        for (int i = 0; i < alreadyGenerated.length; i++)
        {
            alreadyGenerated[i] = new LinkedHashSet<Generated>();
        }
    }

    public void process() throws Exception
    {
        generateModuleClass();
    }

    private void generateModuleClass() throws Exception
    {
//        sourceWriters[SERVER_SOURCE_WRITER] = createSourceWriter("Server");
        sourceWriters[JAVA_CLIENT_SOURCE_WRITER] = createSourceWriter("JavaClient");
        sourceWriters[GWT_SOURCE_WRITER] = createSourceWriter("Gwt");
        Set<String> interfaces = new LinkedHashSet<String>();
        final File allserviceList = AnnotationInjectionProcessor.readInterfacesInto(interfaces, processingEnvironment);
        for (String interfaceName : interfaces)
        {
            createMethods(interfaceName, allserviceList);
        }
        for (SourceWriter moduleWriter : sourceWriters)
        {
            if(moduleWriter == null)
                continue;
            moduleWriter.outdent();
            moduleWriter.println("}");
            moduleWriter.close();
        }
    }

    private SourceWriter createSourceWriter(String type) throws IOException
    {
        final JavaFileObject source = processingEnvironment.getFiler().createSourceFile("org.rapla.dagger.Dagger" + type + "Module");
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package org.rapla.dagger;");
        moduleWriter.println();
        moduleWriter.println("import " + Provides.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provides.Type.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Module.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Factory.class.getCanonicalName() + ";");
        moduleWriter.println("import " + DaggerMapKey.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Singleton.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Exception.class.getCanonicalName() + ";");
//        moduleWriter.println("import " + Provider.class.getCanonicalName() + ";");
        moduleWriter.println();
        moduleWriter.println("@Module");
        moduleWriter.println("public class Dagger" + type + "Module {");
        moduleWriter.indent();
        return moduleWriter;
    }

    private void createMethods(String interfaceName, File allserviceList) throws Exception
    {
        final Set<String> implementingClasses = AnnotationInjectionProcessor.readFileContent(interfaceName, allserviceList);
        for (String implementingClass : implementingClasses)
        {
            final TypeElement implementingClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(implementingClass);
            final Generated generated = new Generated(interfaceName, implementingClass);
            if (implementingClass.endsWith(ProxyCreator.PROXY_SUFFIX) && !alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
            {// Generated Json Proxies
                alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                String interaceNameWithoutPackage = extractNameWithoutPackage(interfaceName);
                final String implementingClassWithoutPackage = extractNameWithoutPackage(implementingClass);
                sourceWriters[GWT_SOURCE_WRITER].println();
                sourceWriters[GWT_SOURCE_WRITER].println("@Provides");
                sourceWriters[GWT_SOURCE_WRITER]
                        .println("public " + interfaceName + " provide_" + interaceNameWithoutPackage + "_" + implementingClassWithoutPackage + "() {");
                sourceWriters[GWT_SOURCE_WRITER].indent();
                sourceWriters[GWT_SOURCE_WRITER].println("return new " + implementingClass + "();");
                sourceWriters[GWT_SOURCE_WRITER].outdent();
                sourceWriters[GWT_SOURCE_WRITER].println("}");
            }
            else if (implementingClassTypeElement != null)
            {
                final DefaultImplementationRepeatable defaultImplementationRepeatable = implementingClassTypeElement
                        .getAnnotation(DefaultImplementationRepeatable.class);
                if (defaultImplementationRepeatable != null)
                {
                    final DefaultImplementation[] defaultImplementations = defaultImplementationRepeatable.value();
                    for (DefaultImplementation defaultImplementation : defaultImplementations)
                    {
                        generate(implementingClassTypeElement, interfaceName, defaultImplementation);
                    }
                }
                final DefaultImplementation defaultImplementation = implementingClassTypeElement.getAnnotation(DefaultImplementation.class);
                if (defaultImplementation != null)
                {
                    generate(implementingClassTypeElement, interfaceName, defaultImplementation);
                }
                final ExtensionRepeatable extensionRepeatable = implementingClassTypeElement.getAnnotation(ExtensionRepeatable.class);
                if (extensionRepeatable != null)
                {
                    final Extension[] extensions = extensionRepeatable.value();
                    for (Extension extension : extensions)
                    {
                        generate(implementingClassTypeElement, interfaceName, extension);
                    }
                }
                final Extension extension = implementingClassTypeElement.getAnnotation(Extension.class);
                if (extension != null)
                {
                    generate(implementingClassTypeElement, interfaceName, extension);
                }
            }
        }
    }

    private void generate(TypeElement implementingClassTypeElement, String interfaceName, DefaultImplementation defaultImplementation)
    {
        final InjectionContext[] context = defaultImplementation.context();
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final String defaultImplClassName = typeUtils.asElement(typeUtils.erasure(implementingClassTypeElement.asType())).getSimpleName().toString();
        final Generated generated = new Generated(interfaceName, defaultImplClassName);
        final TypeElement defaultImplementationOf = getDefaultImplementationOf(defaultImplementation);
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeOf = typeUtils.erasure(defaultImplementationOf.asType());
        final boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeOf);
        if (defaultImplementation != null && assignable)
        {
            final String interfaceNameWithoutPackage = extractNameWithoutPackage(interfaceName);
            final ExecutableElement constructor = getConstructor(implementingClassTypeElement);
            final List<? extends VariableElement> parameters = constructor.getParameters();
            final boolean isSingleton = implementingClassTypeElement.getAnnotation(Singleton.class) != null;
            if (InjectionContext.isInjectableOnGwt(context))
            {
                if (!alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[GWT_SOURCE_WRITER];
                    generateDefaultImplementation(implementingClassTypeElement, interfaceName, defaultImplClassName, interfaceNameWithoutPackage, parameters,
                            isSingleton, moduleWriter);
                }
            }
//            if (InjectionContext.isInjectableOnServer(context))
//            {
//                if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
//                {
//                    alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
//                    SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
//                    generateDefaultImplementation(implementingClassTypeElement, interfaceName, defaultImplClassName, interfaceNameWithoutPackage, parameters,
//                            isSingleton, moduleWriter);
//                }
//            }
//            if (InjectionContext.isInjectableOnSwing(context))
//            {
//                if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
//                {
//                    alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
//                    SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
//                    generateDefaultImplementation(implementingClassTypeElement, interfaceName, defaultImplClassName, interfaceNameWithoutPackage, parameters,
//                            isSingleton, moduleWriter);
//                }
//            }

        }
    }

    private void generateDefaultImplementation(TypeElement implementingClassTypeElement, String interfaceName, final String defaultImplClassName,
            final String interfaceNameWithoutPackage, final List<? extends VariableElement> parameters, final boolean isSingleton, SourceWriter moduleWriter)
    {
        moduleWriter.println();
        moduleWriter.println("@Provides");
        if (isSingleton)
            moduleWriter.println("@Singleton");
        moduleWriter.println("public " + interfaceName + " provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName + "("
                + createString(parameters, true) + ") {");
        moduleWriter.indent();
        moduleWriter.println("try {");
        moduleWriter.indent();
        moduleWriter.println("return new " + implementingClassTypeElement.getQualifiedName().toString() + "(" + createString(parameters, false) + ");");
        moduleWriter.outdent();
        moduleWriter.println("} catch (Exception e) {");
        moduleWriter.indent();
        moduleWriter.println("throw new RuntimeException(e.getMessage(), e);");
        moduleWriter.outdent();
        moduleWriter.println("}");
        moduleWriter.outdent();
        moduleWriter.println("}");
//        moduleWriter.println();
//        moduleWriter.println("@Provides");
//        if (isSingleton)
//            moduleWriter.println("@Singleton");
//        moduleWriter.println("public Provider<" + interfaceName + "> provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName + "_Prov ("
//                + createString(parameters, true) + ") {");
//        moduleWriter.indent();
//        moduleWriter.println("return new Provider<"+interfaceName+">() {");
//        moduleWriter.indent();
//        moduleWriter.println("public "+interfaceName+" get() {");
//        moduleWriter.indent();
//        moduleWriter.println("try {");
//        moduleWriter.indent();
//        moduleWriter.println("return new " + implementingClassTypeElement.getQualifiedName().toString() + "(" + createString(parameters, false) + ");");
//        moduleWriter.outdent();
//        moduleWriter.println("} catch (Exception e) {");
//        moduleWriter.indent();
//        moduleWriter.println("throw new RuntimeException(e.getMessage(), e);");
//        moduleWriter.outdent();
//        moduleWriter.println("}");
//        moduleWriter.outdent();
//        moduleWriter.println("}");
//        moduleWriter.outdent();
//        moduleWriter.println("};");
//        moduleWriter.outdent();
//        moduleWriter.println("}");
    }

    private void generate(TypeElement implementingClassTypeElement, String interfaceName, Extension extension)
    {
        if (extension == null)
        {
            return;
        }
        final TypeElement interfaceProvided = getProvides(extension);
        final ExtensionPoint extensionPoint = interfaceProvided.getAnnotation(ExtensionPoint.class);
        if (extensionPoint == null)
        {
            return;
        }
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeInterface = typeUtils.erasure(interfaceProvided.asType());
        final boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeInterface);
        if (!assignable)
        {
            return;
        }
        final InjectionContext[] context = extensionPoint.context();
        final String interfaceNameWithoutPackage = extractNameWithoutPackage(interfaceName);
        final String defaultImplClassName = typeUtils.asElement(typeUtils.erasure(implementingClassTypeElement.asType())).getSimpleName().toString();
        final ExecutableElement constructor = getConstructor(implementingClassTypeElement);
        final List<? extends VariableElement> parameters = constructor.getParameters();
        final String qualifiedName = implementingClassTypeElement.getQualifiedName().toString();
        final boolean isSingleton = implementingClassTypeElement.getAnnotation(Singleton.class) != null;
        final Generated generated = new Generated(interfaceName, defaultImplClassName);
        if (InjectionContext.isInjectableOnGwt(context))
        {
            if (!alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[GWT_SOURCE_WRITER];
                generateExtensionMethods(interfaceName, extension, interfaceNameWithoutPackage, defaultImplClassName, parameters, qualifiedName, isSingleton,
                        moduleWriter);
            }
        }
//        if (InjectionContext.isInjectableOnServer(context))
//        {
//            if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
//            {
//                alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
//                SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
//                generateExtensionMethods(interfaceName, extension, interfaceNameWithoutPackage, defaultImplClassName, parameters, qualifiedName, isSingleton,
//                        moduleWriter);
//            }
//        }
//        if (InjectionContext.isInjectableOnSwing(context))
//        {
//            if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
//            {
//                alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
//                SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
//                generateExtensionMethods(interfaceName, extension, interfaceNameWithoutPackage, defaultImplClassName, parameters, qualifiedName, isSingleton,
//                        moduleWriter);
//            }
//        }
    }

    private void generateExtensionMethods(String interfaceName, Extension extension, final String interfaceNameWithoutPackage,
            final String defaultImplClassName, final List<? extends VariableElement> parameters, final String qualifiedName, final boolean isSingleton,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        if (isSingleton)
        {
            final String fullQualifiedName = "provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName;
            moduleWriter.println("private " + qualifiedName + " " + fullQualifiedName + "=null;");
        }
        writeMethod(interfaceName, moduleWriter, interfaceNameWithoutPackage, defaultImplClassName, parameters, qualifiedName, isSingleton, true, extension);
        writeMethod(interfaceName, moduleWriter, interfaceNameWithoutPackage, defaultImplClassName, parameters, qualifiedName, isSingleton, false, extension);
    }

    private void writeMethod(String interfaceName, SourceWriter moduleWriter, final String interfaceNameWithoutPackage, final String defaultImplClassName,
            final List<? extends VariableElement> parameters, final String qualifiedName, final boolean isSingleton, final boolean isMap, final Extension extension)
    {
        final String methodSuffix;
        if(isMap)
        {
            moduleWriter.println("@Provides(type=Type.MAP)");
            moduleWriter.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + extension.id() + "\")");
            final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = id+"_Map";
        }
        else
        {
            moduleWriter.println();
            moduleWriter.println("@Provides(type=Type.SET)");
            final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = id+"_Set";
        }
        final String fullQualifiedName = "provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName;
        if (isSingleton)
            moduleWriter.println("@Singleton");
        moduleWriter.println("public " + interfaceName + " " + fullQualifiedName + "_" + methodSuffix + "(" + createString(parameters, true) + ") {");
        moduleWriter.indent();
        moduleWriter.println("try {");
        moduleWriter.indent();
        if (isSingleton)
        {
            moduleWriter.println("if (" + fullQualifiedName + "!=null) {");
            moduleWriter.indent();
            moduleWriter.println("return " + fullQualifiedName + ";");
            moduleWriter.outdent();
            moduleWriter.println("}");
        }
        String variable = "result";
        moduleWriter.println(qualifiedName + " " + variable + " = new " + qualifiedName + "(" + createString(parameters, false) + ");");
        if (isSingleton)
        {
            moduleWriter.println(fullQualifiedName + "=" + variable + ";");
        }
        moduleWriter.println("return " + variable + ";");
        moduleWriter.outdent();
        moduleWriter.println("} catch (Exception e) {");
        moduleWriter.indent();
        moduleWriter.println("throw new RuntimeException(e.getMessage(), e);");
        moduleWriter.outdent();
        moduleWriter.println("}");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private static String extractNameWithoutPackage(String className)
    {
        final int lastIndexOf = className.lastIndexOf(".");
        if (lastIndexOf >= 0)
        {
            return className.substring(lastIndexOf + 1);
        }
        return className;
    }

    private TypeElement getProvides(Extension annotation)
    {
        try
        {
            annotation.provides(); // this should throw
        }
        catch (MirroredTypeException mte)
        {
            return asTypeElement(mte.getTypeMirror());
        }
        return null; // can this ever happen ??
    }

    private TypeElement getDefaultImplementationOf(DefaultImplementation annotation)
    {
        try
        {
            annotation.of(); // this should throw
        }
        catch (MirroredTypeException mte)
        {
            return asTypeElement(mte.getTypeMirror());
        }
        return null; // can this ever happen ??
    }

    private TypeElement asTypeElement(TypeMirror typeMirror)
    {
        Types TypeUtils = processingEnvironment.getTypeUtils();
        return (TypeElement) TypeUtils.asElement(typeMirror);
    }

    private String createString(List<? extends VariableElement> parameters, boolean withType)
    {
        final StringBuilder sb = new StringBuilder();
        if (parameters != null)
        {
            boolean first = true;
            for (Element parameter : parameters)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(", ");
                }
                if (withType)
                {
                    TypeMirror asType = parameter.asType();
                    final Types typeUtils = processingEnvironment.getTypeUtils();
                    if (asType instanceof PrimitiveType)
                    {
                        final TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType)asType);
                        final String string = boxedClass.toString();
                        sb.append(string);
                    }
                    else
                    {
                        final TypeMirror erasure = hasUnboundType(asType) ? typeUtils.erasure(asType):asType;
                        sb.append(erasure.toString());
                    }
                    sb.append(" ");
                }
                sb.append(parameter.getSimpleName().toString());
            }
        }
        return sb.toString();
    }

    private boolean hasUnboundType(TypeMirror asType)
    {
        if (asType instanceof DeclaredType)
        {
            List<? extends TypeMirror> typeParameters = ((DeclaredType) asType).getTypeArguments();
            for (TypeMirror typeMirror : typeParameters)
            {
                if(hasUnboundType(typeMirror))
                {
                    return true;
                }
            }
            return false;
        }
        else
        {
            return !asType.toString().contains(".");
        }
    }

    private ExecutableElement getConstructor(TypeElement element)
    {
        final List<? extends Element> allMembers = processingEnvironment.getElementUtils().getAllMembers(element);
        final List<ExecutableElement> constructors = ElementFilter.constructorsIn(allMembers);
        ExecutableElement foundConstructor = null;
        if (constructors != null)
        {
            for (ExecutableElement constructor : constructors)
            {
                final boolean hasInjectAnnotation = constructor.getAnnotation(Inject.class) != null;
//                        || constructor.getAnnotation(com.google.inject.Inject.class) != null;
                if (hasInjectAnnotation)
                {
                    foundConstructor = constructor;
                    break;
                }
            }
        }
        if (foundConstructor == null)
        {
            throw new IllegalStateException("No @Inject constructor found for: " + element.getQualifiedName().toString());
        }
        return foundConstructor;
    }
}
