package org.rapla.dagger;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.internal.Factory;
import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.annotation.JavaClientProxyCreator;
import org.rapla.gwtjsonrpc.annotation.ProxyCreator;
import org.rapla.gwtjsonrpc.annotation.SourceWriter;
import org.rapla.inject.*;
import org.rapla.inject.generator.AnnotationInjectionProcessor;
import org.rapla.inject.server.RequestModule;
import org.rapla.inject.server.RequestScoped;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

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

    private Set<TypeElement> remoteMethods = new LinkedHashSet<>();
    private static final int SERVER_SOURCE_WRITER = 0;
    private static final int JAVA_CLIENT_SOURCE_WRITER = 1;
    private static final int GWT_SOURCE_WRITER = 2;
    private static final int WEBSERVICE_COMPONENT_WRITER = 3;
    private final Set<Generated>[] alreadyGenerated = new LinkedHashSet[4];
    private final SourceWriter[] sourceWriters = new SourceWriter[4];
    private final ProcessingEnvironment processingEnvironment;
    private final String generatorName;
    public DaggerModuleProcessor(ProcessingEnvironment processingEnvironment, String generatorName)
    {
        super();
        this.generatorName = generatorName;
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
        remoteMethods.clear();;
        sourceWriters[SERVER_SOURCE_WRITER] = createSourceWriter("Server");
        sourceWriters[JAVA_CLIENT_SOURCE_WRITER] = createSourceWriter("JavaClient");
        sourceWriters[GWT_SOURCE_WRITER] = createSourceWriter("Gwt");
        sourceWriters[WEBSERVICE_COMPONENT_WRITER] = createComponentSourceWriter("Webservice");
        Set<String> interfaces = new LinkedHashSet<String>();
        final File allserviceList = AnnotationInjectionProcessor.readInterfacesInto(interfaces, processingEnvironment);
        for (String interfaceName : interfaces)
        {
            createMethods(interfaceName, allserviceList);
        }
        writeWebserviceList("Webservice");
        for (SourceWriter moduleWriter : sourceWriters)
        {
            moduleWriter.outdent();
            moduleWriter.println("}");
            moduleWriter.close();
        }
    }

    private void writeWebserviceList(String type)
    {
        SourceWriter writer = sourceWriters[WEBSERVICE_COMPONENT_WRITER];
        writer.println("ServiceList getList();");
        writer.println("@Singleton");
        writer.println("public static class ServiceList {");
        writer.indent();
        final String componentName = "Dagger" + type + "Component";
        writer.println("final " + componentName + " component;");
        writer.println("@Inject");
        writer.println("ServiceList(final " + componentName + " component) { this.component = component;}");
        writer.println("public <T> Provider<T> find(HttpServletRequest request, HttpServletResponse response,Class<T> clazz) {");
        writer.indent();
        writer.println("return (Provider) find(request,response, clazz.getCanonicalName());");
        writer.outdent();
        writer.println("}");
        writer.println("public Provider<Object> find(HttpServletRequest request, HttpServletResponse response,String servicename) {");
        writer.indent();
        writer.println("RequestModule requestModule = new RequestModule(request, response);");
        writer.println("switch (servicename) {");
        writer.indent();
        for ( TypeElement method:remoteMethods)
        {
            String interfaceName = method.getQualifiedName().toString();
            String methodName = toJavaName(method).toLowerCase();
            writer.println("case \"" +interfaceName + "\": return component." + methodName +"(requestModule);");
        }
        writer.println("default: return null;");
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("}");
        /*
        ServiceList getList();
        @Singleton
        public static class ServiceList
        {
            final WebserviceComponent component;
            @Inject
            public ServiceList(final WebserviceComponent component)
            {
                this.component = component;
            }
            public Provider<Object> find(HttpServletRequest request, HttpServletResponse response,String servicename)
            {
                RequestModule requestModule = new RequestModule(request, response);
                switch (servicename)
                {
                    case "org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest": return component.annotationProcessingTest(requestModule);
                    case "org.rapla.gwtjsonrpc.annotation.AnnotationSimpleProcessingTest": return component.annotationSimpleProcessingTest(requestModule);
                    default return null;
                }
            }
        }
        */


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
        moduleWriter.println();
        moduleWriter.println(getGeneratorString());
        moduleWriter.println("@Module");
        moduleWriter.println("public class Dagger" + type + "Module {");
        moduleWriter.indent();
        return moduleWriter;
    }

    private SourceWriter createComponentSourceWriter(String type) throws IOException
    {
        final JavaFileObject source = processingEnvironment.getFiler().createSourceFile(getComponentName(type));
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package org.rapla.dagger;");
        moduleWriter.println();
        moduleWriter.println("import " + HttpServletRequest.class.getCanonicalName() + ";");
        moduleWriter.println("import " + HttpServletResponse.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provides.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Inject.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provider.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Singleton.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provides.Type.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Subcomponent.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Module.class.getCanonicalName() + ";");
        moduleWriter.println("import " + RequestModule.class.getCanonicalName() + ";");
        moduleWriter.println("import " + DaggerMapKey.class.getCanonicalName() + ";");
        moduleWriter.println("import " + RequestScoped.class.getCanonicalName() + ";");
        moduleWriter.println();
        moduleWriter.println(getGeneratorString());
        moduleWriter.println("@Singleton");
        moduleWriter.println("@Subcomponent");
        moduleWriter.println("public interface Dagger" + type + "Component {");
        moduleWriter.indent();
        return moduleWriter;
    }

    private String getComponentName(String type)
    {
        return "org.rapla.dagger.Dagger" + type + "Component";
    }

    private void createMethods(String interfaceName, File allserviceList) throws Exception
    {
        final Set<String> implementingClasses = AnnotationInjectionProcessor.readFileContent(interfaceName, allserviceList);
        for (String implementingClass : implementingClasses)
        {
            final TypeElement implementingClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(implementingClass);
            final TypeElement interfaceClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(interfaceName);
            if (implementingClass.endsWith(ProxyCreator.PROXY_SUFFIX) )
            {
                generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, GWT_SOURCE_WRITER);
            }
            else if (implementingClass.endsWith(JavaClientProxyCreator.PROXY_SUFFIX) )
            {
                generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, JAVA_CLIENT_SOURCE_WRITER);
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
                        generate(implementingClassTypeElement, interfaceClassTypeElement, defaultImplementation);
                    }
                }
                final DefaultImplementation defaultImplementation = implementingClassTypeElement.getAnnotation(DefaultImplementation.class);
                if (defaultImplementation != null)
                {
                    generate(implementingClassTypeElement, interfaceClassTypeElement, defaultImplementation);
                }
                final ExtensionRepeatable extensionRepeatable = implementingClassTypeElement.getAnnotation(ExtensionRepeatable.class);
                if (extensionRepeatable != null)
                {
                    final Extension[] extensions = extensionRepeatable.value();
                    for (Extension extension : extensions)
                    {
                        generate(implementingClassTypeElement, interfaceClassTypeElement, extension);
                    }
                }
                final Extension extension = implementingClassTypeElement.getAnnotation(Extension.class);
                if (extension != null)
                {
                    generate(implementingClassTypeElement, interfaceClassTypeElement, extension);
                }
            }
        }
    }

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    private void generateProxyMethods(String interfaceName, String implementingClass, TypeElement interfaceClassTypeElement, int sourceWriterIndex)
    {
        final Generated generated = new Generated(interfaceName, implementingClass);
        if (!alreadyGenerated[sourceWriterIndex].contains(generated))
        {
            alreadyGenerated[sourceWriterIndex].add(generated);
            sourceWriters[sourceWriterIndex].println();
            sourceWriters[sourceWriterIndex].println("@Provides");
            sourceWriters[sourceWriterIndex].println(
                    "public " + interfaceName + " provide_" + toJavaName(interfaceClassTypeElement) + "_" + implementingClass.replaceAll("\\.", "_")
                            + "(" + implementingClass + " result) {");
            sourceWriters[sourceWriterIndex].indent();
            sourceWriters[sourceWriterIndex].println("return result;");
            sourceWriters[sourceWriterIndex].outdent();
            sourceWriters[sourceWriterIndex].println("}");
        }
    }

    private String toJavaName(TypeElement className)
    {
        final String s = className.getQualifiedName().toString().replaceAll("\\.", "_");
        return s;
    }

    private String firstCharUp(String s)
    {
        if ( s == null)
        {
            return null;
        }
        if ( s.length() <1)
        {
            return s;
        }
        final String result = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return result;
    }


    private void generate(TypeElement implementingClassTypeElement, TypeElement interfaceTypeElement, DefaultImplementation defaultImplementation)
    {
        final InjectionContext[] context = defaultImplementation.context();
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(), implementingClassTypeElement.getQualifiedName().toString());
        final TypeElement defaultImplementationOf = getDefaultImplementationOf(defaultImplementation);
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeOf = typeUtils.erasure(defaultImplementationOf.asType());
        final boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeOf);
        if (defaultImplementation != null && assignable)
        {
            if (InjectionContext.isInjectableOnGwt(context))
            {
                if (!alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[GWT_SOURCE_WRITER];
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnServer(context))
            {
                if ( interfaceTypeElement.getAnnotation(RemoteJsonMethod.class) != null)
                {
                    if (!alreadyGenerated[WEBSERVICE_COMPONENT_WRITER].contains(generated))
                    {
                        alreadyGenerated[WEBSERVICE_COMPONENT_WRITER].add(generated);
                        SourceWriter moduleWriter = sourceWriters[WEBSERVICE_COMPONENT_WRITER];
                        generateWebserviceComponent(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    }
                }
                if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnSwing(context))
            {
                if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                }
            }

        }
    }

    private void generateDefaultImplementation(TypeElement implementingClassTypeElement, TypeElement interfaceName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        moduleWriter.println("@Provides");

        String implementingName = implementingClassTypeElement.getQualifiedName().toString();
        moduleWriter.println("public " + interfaceName + " provide_" + toJavaName(interfaceName) + "_" + toJavaName(implementingClassTypeElement) + "("
                + implementingName + " result) {");
        moduleWriter.indent();
        moduleWriter.println("return result;");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    /*
     AnnotationProcessingTest_ annotationProcessingTest(RequestModule module);
    @RequestScoped
    @Subcomponent(modules = RequestModule.class)
    interface AnnotationSimpleProcessingTest_ extends Provider<Object>
    {
        AnnotationSimpleProcessingTestImpl get();
     */
    private void generateWebserviceComponent(TypeElement implementingClassTypeElement, TypeElement interfaceName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        //final String simpleName = interfaceName.getQualifiedName().toString();
        final String methodName = toJavaName(interfaceName).toLowerCase();
        // it is important that the first char is a Uppercase latter otherwise dagger fails with IllegalArgumentException
        final String serviceComponentName = firstCharUp(toJavaName(interfaceName)) +"Service";
        moduleWriter.println(serviceComponentName + " " + methodName + "(RequestModule module);");
        moduleWriter.println("@RequestScoped");
        moduleWriter.println("@Subcomponent(modules=RequestModule.class)");

        String implementingName = implementingClassTypeElement.getQualifiedName().toString();
        remoteMethods.add( interfaceName);
        moduleWriter.println("interface " + serviceComponentName + " extends Provider<Object> {");
        moduleWriter.indent();
        moduleWriter.println(implementingName + " get();");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private void generate(TypeElement implementingClassTypeElement, TypeElement interfaceElementType, Extension extension)
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
        final TypeElement defaultImpl =(( TypeElement)typeUtils.asElement(typeUtils.erasure(implementingClassTypeElement.asType())));
        final Generated generated = new Generated(interfaceElementType.getQualifiedName().toString(), defaultImpl.getQualifiedName().toString());
        if (InjectionContext.isInjectableOnGwt(context))
        {
            if (!alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[GWT_SOURCE_WRITER];
                generateExtensionMethods(extension,interfaceElementType , defaultImpl,moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnServer(context))
        {
            if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
                generateExtensionMethods(extension,interfaceElementType , defaultImpl,moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnSwing(context))
        {
            if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
                generateExtensionMethods(extension,interfaceElementType , defaultImpl,moduleWriter);
            }
        }
    }

    private void generateExtensionMethods(Extension extension, final TypeElement interfaceName,final TypeElement defaultImplClassName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
//        if (isSingleton)
//        {
//            final String fullQualifiedName = "provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName;
//            moduleWriter.println("private " + qualifiedName + " " + fullQualifiedName + "=null;");
//        }
        writeMethod(interfaceName, moduleWriter,  defaultImplClassName,  true, extension);
        writeMethod(interfaceName, moduleWriter, defaultImplClassName,   false, extension);
    }

    private void writeMethod(TypeElement interfaceName, SourceWriter moduleWriter,  final TypeElement defaultImplClassName,
              final boolean isMap, final Extension extension)
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
        final String fullQualifiedName = "provide_" + toJavaName(interfaceName) + "_" + toJavaName(defaultImplClassName);
        moduleWriter.println("public " + interfaceName + " " + fullQualifiedName + "_" + methodSuffix + "(" + defaultImplClassName.getQualifiedName().toString() + " impl) {");
        moduleWriter.indent();
        moduleWriter.println("return impl;");
        moduleWriter.outdent();
        moduleWriter.println("}");
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


}
