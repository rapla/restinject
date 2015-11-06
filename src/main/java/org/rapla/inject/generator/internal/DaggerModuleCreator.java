package org.rapla.inject.generator.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
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
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.ComponentStarter;
import org.rapla.inject.generator.AnnotationInjectionProcessor;
import org.rapla.inject.internal.DaggerMapKey;
import org.rapla.inject.server.RequestScoped;
import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.generator.internal.GwtProxyCreator;
import org.rapla.jsonrpc.generator.internal.JavaClientProxyCreator;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;

public class DaggerModuleCreator
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

        @Override public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
            return result;
        }

        @Override public boolean equals(Object obj)
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
    private static final int REQUEST_SOURCE_WRITER = 4;
    private final Set<Generated>[] alreadyGenerated = new LinkedHashSet[5];
    private final SourceWriter[] sourceWriters = new SourceWriter[5];
    private final ProcessingEnvironment processingEnvironment;
    private final String generatorName;

    public DaggerModuleCreator(ProcessingEnvironment processingEnvironment, String generatorName)
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
        String packageName = "";
        String fileName = "moduleDescription";
        FileObject resource;
        {
            JavaFileManager.Location loc = StandardLocation.SOURCE_OUTPUT;
            resource = processingEnvironment.getFiler().getResource(loc, packageName, fileName);
        }
        if ( !new File(resource.toUri()).exists())
        {
            JavaFileManager.Location loc = StandardLocation.CLASS_PATH;
            resource = processingEnvironment.getFiler().getResource(loc, packageName, fileName);
        }
        if ( !new File(resource.toUri()).exists())
        {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Module find not found " + packageName + (packageName.length() > 0 ? "/" : "") +fileName);
            return;
         //   JavaFileManager.Location loc = StandardLocation.CLASS_PATH;
//            resource = processingEnvironment.getFiler().getResource(loc, packageName, fileName);
        }
        String moduleName;
        try (final BufferedReader reader = new BufferedReader(resource.openReader(true)))
        {
            moduleName = reader.readLine();
        }
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Module " + moduleName);
        generateModuleClass(moduleName);
    }

    private void generateModuleClass(String moduleName) throws Exception
    {
        final int i = moduleName.lastIndexOf(".");
        String packageName = (i >=0 ? moduleName.substring(0, i) : "" ) ;
        packageName+= (packageName.length() == 0 ? "" :".") + "dagger";
        String artifactName = firstCharUp(i >=0 ? moduleName.substring(i+1) : moduleName);
        remoteMethods.clear();
        sourceWriters[SERVER_SOURCE_WRITER] = createSourceWriter(packageName, artifactName, "Server");
        sourceWriters[JAVA_CLIENT_SOURCE_WRITER] = createSourceWriter(packageName, artifactName, "JavaClient");
        sourceWriters[GWT_SOURCE_WRITER] = createSourceWriter(packageName, artifactName, "Gwt");
        sourceWriters[WEBSERVICE_COMPONENT_WRITER] = createWebserviceComponentSourceWriter(packageName, artifactName);
        sourceWriters[REQUEST_SOURCE_WRITER] = createRequestModuleSourceWriter(packageName, artifactName);

        Set<String> interfaces = new LinkedHashSet<String>();
        final File allserviceList = AnnotationInjectionProcessor.readInterfacesInto(interfaces, processingEnvironment);
        for (String interfaceName : interfaces)
        {
            createMethods(interfaceName, allserviceList,artifactName);
        }
        writeWebserviceList(artifactName);
        for (SourceWriter moduleWriter : sourceWriters)
        {
            moduleWriter.outdent();
            moduleWriter.println("}");
            moduleWriter.close();
        }
        generateComponents();
    }

    /**
     * Generates the dagger component interfaces for Server, Swing and GWT.</br> 
     * An example for a server component:</br>
     * <code>package org.rapla.server.internal.dagger;</br>
     * import javax.inject.Singleton;</br>
     * import org.rapla.dagger.DaggerServerModule;</br>
     * import org.rapla.dagger.DaggerWebserviceComponent;</br>
     * import org.rapla.server.internal.ServerServiceImpl;</br></code>
     * <code>@Singleton @dagger.Component(modules = { DaggerServerModule.class, MyModule.class })</br>
     * public interface ServerComponent</br>
     * {</br>
     *   DaggerWebserviceComponent getWebservices0();</br>
     *   Starter getStarter();</br>
     * }</code></br>
     * </br>An example for swing:</br>
     * <code>@Singleton @dagger.Component(modules = { DaggerJavaClientModule.class, MyClientModule.class })</br>
     * public interface ClientComponent</br>
     * {</br>
     *    RaplaClientServiceImpl getClient();</br>
     * }</br>
     * </code>
     */
    private void generateComponents() throws Exception
    {
        {
            SourceWriter sourceWriter = createComponentSourceWriter("org.rapla.server.dagger", "Server");
            {// create dagger webservice components
                int i = 0;
                final List<String> webserviceComponents = findModules("DaggerWebserviceComponent", "org.rapla.DaggerWebserviceComponents");
                for (String webserviceComponent : webserviceComponents)
                {
                    sourceWriter.println(webserviceComponent + " getWebservices" + i + "();");
                    i++;
                }
            }
            {// create starter
                sourceWriter.println(ComponentStarter.class.getSimpleName()+" getStarter();");
            }
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter("org.rapla.client.swing.dagger", "JavaClient");
            {// create starter
                sourceWriter.println(ComponentStarter.class.getSimpleName()+" getStarter();");
            }
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter("org.rapla.client.gwt.dagger", "Gwt");
            {// create starter
                sourceWriter.println(ComponentStarter.class.getSimpleName()+" getStarter();");
            }
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
    }

    private SourceWriter createComponentSourceWriter(String interfacePackage, String interfaceName) throws Exception
    {
        final JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(interfacePackage + "." + interfaceName + "Component");
        final SourceWriter sourceWriter = new SourceWriter(sourceFile.openOutputStream());
        sourceWriter.println("package " + interfacePackage + ";");
        sourceWriter.println();
        sourceWriter.println("import " + ComponentStarter.class.getCanonicalName() + ";");
        sourceWriter.println();
        sourceWriter.print("@" + Singleton.class.getCanonicalName());
        sourceWriter.print(" @" + Component.class.getCanonicalName());
        sourceWriter.print("(modules = {");
        final String modulesName = "org.rapla.Dagger" + interfaceName + "Module";
        final List<String> modules = findModules(interfaceName, modulesName);
        for (String module : modules)
        {
            sourceWriter.print(module);
            sourceWriter.print(".class, ");
        }
        sourceWriter.println("})");
        sourceWriter.println("public interface " + interfaceName + "Component {");
        sourceWriter.indent();
        return sourceWriter;
    }

    private List<String> findModules(String interfaceName, final String modulesName) throws IOException, UnsupportedEncodingException
    {
        List<String> foundModules = new ArrayList<String>();
        boolean found = false;
        final String file = "META-INF/services/" + modulesName;
        final FileObject resource = processingEnvironment.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", file);
        if (resource != null)
        {
            found = true;
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openInputStream(), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                foundModules.add(line);
            }
            reader.close();
        }
        final Enumeration<URL> resources = DaggerModuleCreator.class.getClassLoader().getResources(file);
        if(!found)
        {
            found =resources.hasMoreElements();
        }
        if(!found)
        {
            throw new IllegalStateException("Tried to generate ComponentInterface for "+interfaceName+" but no modules found for "+modulesName);
        }
        while (resources.hasMoreElements())
        {
            final URL nextElement = resources.nextElement();
            BufferedReader reader = new BufferedReader(new InputStreamReader(nextElement.openStream(), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                foundModules.add(line);
            }
            reader.close();
        }
        return foundModules;
    }

    private void writeWebserviceList(String artifact)
    {
        SourceWriter writer = sourceWriters[WEBSERVICE_COMPONENT_WRITER];
        writer.println("ServiceList getList();");
        writer.println("@Singleton");
        writer.println("public static class ServiceList {");
        writer.indent();
        final String componentName = "Dagger" + artifact + "WebserviceComponent";
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
        final String requestModuleName = "Dagger" + artifact + "RequestModule";
        writer.println(requestModuleName + " requestModule = new " + requestModuleName + "(request, response);");
        writer.println("switch (servicename) {");
        writer.indent();
        for (TypeElement method : remoteMethods)
        {
            String interfaceName = method.getQualifiedName().toString();
            String methodName = toJavaName(method).toLowerCase();
            writer.println("case \"" + interfaceName + "\": return component." + methodName + "(requestModule);");
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
                    case "org.rapla.gwtjsonrpc.proxy.AnnotationProcessingTest": return component.annotationProcessingTest(requestModule);
                    case "org.rapla.gwtjsonrpc.proxy.AnnotationSimpleProcessingTest": return component.annotationSimpleProcessingTest(requestModule);
                    default return null;
                }
            }
        }
        */

    }

    private SourceWriter createSourceWriter(String packageName, String prefix, String type) throws IOException
    {
        final Filer filer = processingEnvironment.getFiler();
        final String className = packageName + ".Dagger" + prefix + type + "Module";
        if (type != null && !type.isEmpty())
        {
            final File allserviceList = AnnotationInjectionProcessor.getFile(processingEnvironment.getFiler());
            AnnotationInjectionProcessor.addServiceFile("org.rapla.Dagger" + type + "Module", allserviceList, className);
        }
        final JavaFileObject source = filer.createSourceFile(className);
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package " + packageName + ";");
        moduleWriter.println();
        if (type.toLowerCase().contains("request"))
        {
            moduleWriter.println("import " + HttpServletRequest.class.getCanonicalName() + ";");
            moduleWriter.println("import " + HttpServletResponse.class.getCanonicalName() + ";");
        }
        moduleWriter.println("import " + Provides.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provides.Type.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Module.class.getCanonicalName() + ";");
//        moduleWriter.println("import " + Factory.class.getCanonicalName() + ";");
        moduleWriter.println("import " + DaggerMapKey.class.getCanonicalName() + ";");
        moduleWriter.println();
        moduleWriter.println(getGeneratorString());
        moduleWriter.println("@Module");
        moduleWriter.println("public class Dagger" + prefix + type + "Module {");
        moduleWriter.indent();
        return moduleWriter;
    }

    private SourceWriter createRequestModuleSourceWriter(String packageName,String artifactName) throws IOException
    {
        String type = artifactName + "Request";
        final SourceWriter writer = createSourceWriter(packageName, artifactName, "Request");

        writer.println("HttpServletRequest request;");
        writer.println("HttpServletResponse response;");
        writer.println("public Dagger" + type + "Module(HttpServletRequest request, HttpServletResponse response){");
        writer.indent();
        writer.println("this.request = request;this.response = response;");
        writer.outdent();
        writer.println("};");
        writer.println("@Provides public HttpServletRequest provideRequest()  {  return request;    }");
        writer.println("@Provides public HttpServletResponse provideResponse(){ return response;     }");
        return writer;
    }

    private SourceWriter createWebserviceComponentSourceWriter(String packageName,String artifcatName) throws IOException
    {
        String type = artifcatName+ "Webservice";
        final String componentName = getComponentName(packageName,type);
        final Filer filer = processingEnvironment.getFiler();
        {// serviceFile filling
            final String serviceFileName = "org.rapla.DaggerWebserviceComponents";
            final File allserviceList = AnnotationInjectionProcessor.getFile(processingEnvironment.getFiler());
            AnnotationInjectionProcessor.addServiceFile(serviceFileName, allserviceList, componentName);
        }
        final JavaFileObject source = filer.createSourceFile(componentName);
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package "+ packageName + ";");
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

    private String getComponentName(String packageName,String type)
    {
        return packageName + ".Dagger" + type + "Component";
    }

    private void createMethods(String interfaceName, File allserviceList, String artifactName) throws Exception
    {
        final Set<String> implementingClasses = AnnotationInjectionProcessor.readFileContent(interfaceName, allserviceList);
        final TypeElement interfaceClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(interfaceName);
        for (String implementingClass : implementingClasses)
        {
            final TypeElement implementingClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(implementingClass);
            if (implementingClass.endsWith(GwtProxyCreator.PROXY_SUFFIX))
            {
                generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, GWT_SOURCE_WRITER);
            }
            else if (implementingClass.endsWith(JavaClientProxyCreator.PROXY_SUFFIX))
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
                        generateDefaultImplementation(artifactName,implementingClassTypeElement, interfaceClassTypeElement, defaultImplementation);
                    }
                }
                final DefaultImplementation defaultImplementation = implementingClassTypeElement.getAnnotation(DefaultImplementation.class);
                if (defaultImplementation != null)
                {
                    generateDefaultImplementation(artifactName,implementingClassTypeElement, interfaceClassTypeElement, defaultImplementation);
                }
                final ExtensionRepeatable extensionRepeatable = implementingClassTypeElement.getAnnotation(ExtensionRepeatable.class);
                if (extensionRepeatable != null)
                {
                    final Extension[] extensions = extensionRepeatable.value();
                    for (Extension extension : extensions)
                    {
                        generateExtension(implementingClassTypeElement, interfaceClassTypeElement, extension);
                    }
                }
                final Extension extension = implementingClassTypeElement.getAnnotation(Extension.class);
                if (extension != null)
                {
                    generateExtension(implementingClassTypeElement, interfaceClassTypeElement, extension);
                }
            }
        }
        if (interfaceClassTypeElement != null)
        {
            final ExtensionPoint annotation = interfaceClassTypeElement.getAnnotation(ExtensionPoint.class);
            if (annotation != null)
            {
                generateDefaultImplementation(interfaceClassTypeElement, annotation);
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
                    "public " + interfaceName + " provide_" + toJavaName(interfaceClassTypeElement) + "_" + implementingClass.replaceAll("\\.", "_") + "("
                            + implementingClass + " result) {");
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
        if (s == null)
        {
            return null;
        }
        if (s.length() < 1)
        {
            return s;
        }
        final String result = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return result;
    }

    private void generateDefaultImplementation(TypeElement interfaceTypeElement, ExtensionPoint annotation)
    {
        final InjectionContext[] context = annotation.context();
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(), "emtpy");
        //final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        //final TypeMirror asTypeOf = typeUtils.erasure(defaultImplementationOf.asType());
        //boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeOf);
        if (InjectionContext.isInjectableOnGwt(context))
        {
            if (!alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[GWT_SOURCE_WRITER];
                generateEmptyExtensionMethods( interfaceTypeElement,  moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnServer(context))
        {
            if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
                generateEmptyExtensionMethods( interfaceTypeElement,  moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnSwing(context))
        {
            if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
                generateEmptyExtensionMethods( interfaceTypeElement,  moduleWriter);
            }
        }

    }

    private void generateEmptyExtensionMethods( final TypeElement interfaceName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        //        if (isSingleton)
        //        {
        //            final String fullQualifiedName = "provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName;
        //            moduleWriter.println("private " + qualifiedName + " " + fullQualifiedName + "=null;");
        //        }
        writeEmptyMethod(interfaceName, moduleWriter );
    }

    private void writeEmptyMethod(TypeElement interfaceName, SourceWriter moduleWriter )
    {
        final String collectionTypeString;
            moduleWriter.println();
            moduleWriter.println("@Provides(type=Type.SET_VALUES)");
            collectionTypeString = "Set";
        final String fullQualifiedName = "provide_" + toJavaName(interfaceName) + "_empty";
        moduleWriter.println("public java.util.Set<" + interfaceName.getQualifiedName().toString() + "> " + fullQualifiedName + "_" + collectionTypeString + "() {");
        moduleWriter.indent();
        moduleWriter.println("return java.util.Collections.emptySet();");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }



    private void generateDefaultImplementation(String artifactName,TypeElement implementingClassTypeElement, TypeElement interfaceTypeElement,
            DefaultImplementation defaultImplementation)
    {
        if(!getDefaultImplementationOf(defaultImplementation).equals(interfaceTypeElement))
        {
            return;
        }
        final InjectionContext[] context = defaultImplementation.context();
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(),
                implementingClassTypeElement.getQualifiedName().toString());
        final TypeElement defaultImplementationOf = getDefaultImplementationOf(defaultImplementation);
        boolean requestScoped = implementingClassTypeElement.getAnnotation(RequestScoped.class) != null;
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeOf = typeUtils.erasure(defaultImplementationOf.asType());
        boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeOf);
        if (!assignable)
        {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING, asTypeImpl.toString() + " can't be assigned to " + asTypeOf);
        }
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
                if (interfaceTypeElement.getAnnotation(RemoteJsonMethod.class) != null)
                {
                    if (!alreadyGenerated[WEBSERVICE_COMPONENT_WRITER].contains(generated))
                    {
                        alreadyGenerated[WEBSERVICE_COMPONENT_WRITER].add(generated);
                        SourceWriter moduleWriter = sourceWriters[WEBSERVICE_COMPONENT_WRITER];
                        generateWebserviceComponent(artifactName,implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    }
                }
                if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[requestScoped ?  REQUEST_SOURCE_WRITER: SERVER_SOURCE_WRITER];
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

    private void generateDefaultImplementation(TypeElement implementingClassTypeElement, TypeElement interfaceName, SourceWriter moduleWriter)
    {
        moduleWriter.println();
        moduleWriter.println("@Provides");

        String implementingName = implementingClassTypeElement.getQualifiedName().toString();
        moduleWriter.println(
                "public " + interfaceName + " provide_" + toJavaName(interfaceName) + "_" + toJavaName(implementingClassTypeElement) + "(" + implementingName
                        + " result) {");
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
    private void generateWebserviceComponent(String artifactName,TypeElement implementingClassTypeElement, TypeElement interfaceName, SourceWriter moduleWriter)
    {
        moduleWriter.println();
        //final String simpleName = interfaceName.getQualifiedName().toString();
        final String methodName = toJavaName(interfaceName).toLowerCase();
        // it is important that the first char is a Uppercase latter otherwise dagger fails with IllegalArgumentException
        final String serviceComponentName = firstCharUp(toJavaName(interfaceName)) + "Service";
        final String daggerRequestModuleName = "Dagger" + artifactName + "RequestModule";
        moduleWriter.println(serviceComponentName + " " + methodName + "("+ daggerRequestModuleName +" module);");
        moduleWriter.println("@RequestScoped");
        moduleWriter.println("@Subcomponent(modules={"+daggerRequestModuleName+".class})");

        String implementingName = implementingClassTypeElement.getQualifiedName().toString();
        remoteMethods.add(interfaceName);
        moduleWriter.println("interface " + serviceComponentName + " extends Provider<Object> {");
        moduleWriter.indent();
        moduleWriter.println(implementingName + " get();");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private void generateExtension(TypeElement implementingClassTypeElement, TypeElement interfaceElementType, Extension extension)
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
        if (implementingClassTypeElement.getAnnotation(RequestScoped.class) != null)
        {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR,"No requestscope allowed on Extensions but used in " + implementingClassTypeElement.toString());
            return;
        }
        final InjectionContext[] context = extensionPoint.context();
        final TypeElement defaultImpl = ((TypeElement) typeUtils.asElement(typeUtils.erasure(implementingClassTypeElement.asType())));
        final Generated generated = new Generated(interfaceElementType.getQualifiedName().toString(), defaultImpl.getQualifiedName().toString());
        if (InjectionContext.isInjectableOnGwt(context))
        {
            if (!alreadyGenerated[GWT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[GWT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[GWT_SOURCE_WRITER];
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnServer(context))
        {
            if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnSwing(context))
        {
            if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
    }

    private void generateExtensionMethods(Extension extension, final TypeElement interfaceName, final TypeElement defaultImplClassName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        //        if (isSingleton)
        //        {
        //            final String fullQualifiedName = "provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName;
        //            moduleWriter.println("private " + qualifiedName + " " + fullQualifiedName + "=null;");
        //        }
        writeMethod(interfaceName, moduleWriter, defaultImplClassName, true, extension);
        writeMethod(interfaceName, moduleWriter, defaultImplClassName, false, extension);
    }

    private void writeMethod(TypeElement interfaceName, SourceWriter moduleWriter, final TypeElement defaultImplClassName, final boolean isMap,
            final Extension extension)
    {
        final String methodSuffix;
        if (isMap)
        {
            moduleWriter.println("@Provides(type=Type.MAP)");
            moduleWriter.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + extension.id() + "\")");
            final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = id + "_Map";
        }
        else
        {
            moduleWriter.println();
            moduleWriter.println("@Provides(type=Type.SET)");
            final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = id + "_Set";
        }
        final String fullQualifiedName = "provide_" + toJavaName(interfaceName) + "_" + toJavaName(defaultImplClassName);
        moduleWriter.println("public " + interfaceName + " " + fullQualifiedName + "_" + methodSuffix + "(" + defaultImplClassName.getQualifiedName().toString()
                + " impl) {");
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
