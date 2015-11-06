package org.rapla.inject.generator.internal;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.generator.AnnotationInjectionProcessor;
import org.rapla.inject.internal.DaggerMapKey;
import org.rapla.inject.internal.server.BasicRequestModule;
import org.rapla.server.RequestScoped;
import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.generator.internal.GwtProxyCreator;
import org.rapla.jsonrpc.generator.internal.JavaClientProxyCreator;
import org.rapla.jsonrpc.server.WebserviceCreator;

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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
        if (!new File(resource.toUri()).exists())
        {
            JavaFileManager.Location loc = StandardLocation.CLASS_PATH;
            resource = processingEnvironment.getFiler().getResource(loc, packageName, fileName);
        }
        if (!new File(resource.toUri()).exists())
        {
            processingEnvironment.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Module find not found " + packageName + (packageName.length() > 0 ? "/" : "") + fileName);
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
        final String packageName = (i >= 0 ? moduleName.substring(0, i) : "");
        final String packageNameWithDagger = packageName + (packageName.length() == 0 ? "" : ".") + "dagger";
        String artifactName = firstCharUp(i >= 0 ? moduleName.substring(i + 1) : moduleName);
        remoteMethods.clear();
        sourceWriters[SERVER_SOURCE_WRITER] = createSourceWriter(packageNameWithDagger, artifactName, "Server");
        sourceWriters[JAVA_CLIENT_SOURCE_WRITER] = createSourceWriter(packageNameWithDagger, artifactName, "JavaClient");
        sourceWriters[GWT_SOURCE_WRITER] = createSourceWriter(packageNameWithDagger, artifactName, "Gwt");
        sourceWriters[WEBSERVICE_COMPONENT_WRITER] = createWebserviceComponentSourceWriter(packageNameWithDagger, artifactName);
        sourceWriters[REQUEST_SOURCE_WRITER] = createSourceWriter(packageNameWithDagger, artifactName, "Request");

        Set<String> interfaces = new LinkedHashSet<String>();
        final File allserviceList = AnnotationInjectionProcessor.readInterfacesInto(interfaces, processingEnvironment);
        Map<String, BitSet> exportedInterfaces = new LinkedHashMap<>();
        for (String interfaceName : interfaces)
        {
            BitSet exportedInterfaceList = createMethods(interfaceName, allserviceList, artifactName);
            exportedInterfaces.put(interfaceName, exportedInterfaceList);
        }
        writeWebserviceList(artifactName);
        for (SourceWriter moduleWriter : sourceWriters)
        {
            moduleWriter.outdent();
            moduleWriter.println("}");
            moduleWriter.close();
        }
        generateComponents(artifactName, packageName, exportedInterfaces);
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
     *    Starter getStarter();</br>
     * }</br>
     * </code>
     * @param artifactName
     * @param packageName
     */
    private void generateComponents(String artifactName, String packageName, Map<String, BitSet> exportedInterfaces) throws Exception
    {
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName + ".server.dagger", artifactName, "Server");
            String componentName = artifactName + "Server" + "Component";
            {// create dagger webservice components
                final Set<String> webserviceComponents = findModules("DaggerWebserviceComponent", "org.rapla.DaggerWebserviceComponents");
                int size = webserviceComponents.size();
                {
                    int i = 0;
                    for (String webserviceComponent : webserviceComponents)
                    {
                        sourceWriter.println(webserviceComponent + " getWebservices" + i + "();");
                        i++;
                    }
                }
                // Example:
                //                ServiceMap getServiceMap();
                //                @Singleton public static class ServiceMap extends LinkedHashMap<String,WebserviceCreator> {
                //                    @Inject public ServiceMap(ServerComponent component) {
                //                        putAll(component.getWebservice().getMap());
                //                        putAll(component.getWebservice2().getMap());
                //                    }
                //                }
                sourceWriter.println("ServiceMap getServiceMap();");
                String webserviceCreatorClass = WebserviceCreator.class.getCanonicalName();
                sourceWriter.println("@Singleton public static class ServiceMap extends java.util.LinkedHashMap<String," + webserviceCreatorClass + "> {");
                sourceWriter.indent();
                sourceWriter.println("@Inject public ServiceMap(" + componentName + " component) {");
                sourceWriter.indent();

                for (int i = 0; i < size; i++)
                {
                    sourceWriter.println("putAll(component.getWebservices" + i + "().getMap());");
                }

                sourceWriter.outdent();
                sourceWriter.println("}");
                sourceWriter.outdent();
                sourceWriter.println("}");

            }
            printExported(exportedInterfaces, sourceWriter, SERVER_SOURCE_WRITER);
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName + ".client.swing.dagger", artifactName, "JavaClient");
            printExported(exportedInterfaces, sourceWriter, JAVA_CLIENT_SOURCE_WRITER);
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName + ".client.gwt.dagger", artifactName, "Gwt");
            printExported(exportedInterfaces, sourceWriter, GWT_SOURCE_WRITER);
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
    }

    private void printExported(Map<String, BitSet> exportedInterfaces, SourceWriter sourceWriter, int sourceWriterIndex)
    {
        for (Map.Entry<String, BitSet> entry : exportedInterfaces.entrySet())
        {// create starter
            String key = entry.getKey();
            BitSet bitSet = entry.getValue();
            if (bitSet.get(sourceWriterIndex))
            {
                final int i = key.lastIndexOf(".");
                String simpleName = i > 0 ? key.substring(i + 1) : key;
                String javaname = firstCharUp(simpleName);
                sourceWriter.println(key + " get" + javaname + "();");
            }
        }
    }

    private SourceWriter createComponentSourceWriter(String packageName,  String artifactId, String type) throws Exception
    {
        final String moduleListFile = "org.rapla.Dagger" + type + "Module";
        final Set<String> modules = findModules(artifactId, moduleListFile);
        final String simpleComponentName = artifactId + type + "Component";
        final String componentName = packageName + "." + simpleComponentName;

        final JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(componentName);
        final SourceWriter sourceWriter = new SourceWriter(sourceFile.openOutputStream());
        sourceWriter.println("package " + packageName + ";");
        sourceWriter.println();
        sourceWriter.println("import " + Inject.class.getCanonicalName() + ";");
        sourceWriter.println("import " + Singleton.class.getCanonicalName() + ";");
        sourceWriter.println("import " + Component.class.getCanonicalName() + ";");
        sourceWriter.println();
        sourceWriter.print("@Singleton @Component(modules = {");
        boolean first = true;
        for (String module : modules)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sourceWriter.print(",");
            }
            sourceWriter.print(module);
            sourceWriter.print(".class");

        }
        sourceWriter.println("})");
        sourceWriter.println("public interface " + simpleComponentName + " {");
        sourceWriter.indent();
        return sourceWriter;
    }

    private Set<String> findModules(String artifactId, final String modulesName) throws IOException
    {
        Set<String> foundModules = new LinkedHashSet<>();
        boolean found = false;
        final String file = "META-INF/services/" + modulesName;
        final FileObject resource = processingEnvironment.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", file);
        if (resource != null)
        {
            found = true;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openInputStream(), "UTF-8"));)
            {
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    foundModules.add(line);
                }
            }
        }
        final Enumeration<URL> resources = DaggerModuleCreator.class.getClassLoader().getResources(file);
        if (!found)
        {
            found = resources.hasMoreElements();
        }
        if (!found)
        {
            throw new IllegalStateException("Tried to generate ComponentInterface for " + artifactId + " but no modules found for " + modulesName);
        }
        while (resources.hasMoreElements())
        {
            final URL nextElement = resources.nextElement();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(nextElement.openStream(), "UTF-8")))
            {
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    foundModules.add(line);
                }
            }
        }
        return foundModules;
    }

    private void writeWebserviceList(String artifact)
    {
        // Example:
        //   ServiceMap getMap();
        //  @Singleton public static class ServiceMap extends LinkedHashMap<String,WebserviceCreator> {
        //    final DaggerRaplaWebserviceComponent component;
        //    @Inject ServiceMap(final DaggerRaplaWebserviceComponent component) {
        //        this.component = component;
        //        put("org.rapla.common.AnnotationProcessingTest");
        //        put("org.rapla.common.AnnotationSimpleProcessingTest");
        //    }
        //    private void put(final String serviceName) {
        //        put(serviceName,new WebserviceCreator()  {
        //                @Override public Object create(HttpServletRequest request, HttpServletResponse response) {
        //                    DaggerRaplaRequestModule requestModule = new DaggerRaplaRequestModule(request, response);
        //                    switch (serviceName) {
        //                        case "org.rapla.common.AnnotationProcessingTest": return component.org_rapla_common_annotationprocessingtest(requestModule).get();
        //                        case "org.rapla.common.AnnotationSimpleProcessingTest": return component.org_rapla_common_annotationsimpleprocessingtest(requestModule).get();
        //                        default: return null;
        //                    }
        //                }
        //            });

        SourceWriter writer = sourceWriters[WEBSERVICE_COMPONENT_WRITER];
        writer.println("ServiceMap getMap();");
        String webserviceCreatorClass = WebserviceCreator.class.getCanonicalName();
        writer.println("@Singleton public static class ServiceMap extends java.util.LinkedHashMap<String," + webserviceCreatorClass + "> {");
        writer.indent();
        final String componentName = "Dagger" + artifact + "WebserviceComponent";
        writer.println("final " + componentName + " component;");
        writer.println("@Inject ServiceMap(final " + componentName + " component) { ");
        writer.indent();
        //final String requestModuleName = "Dagger" + artifact + "RequestModule";
        final String requestModuleName = "BasicRequestModule";
        writer.println("this.component = component;");
        for (TypeElement method : remoteMethods)
        {
            String interfaceName = method.getQualifiedName().toString();
            writer.println("put(\"" + interfaceName + "\");");
        }
        writer.println("}");
        writer.outdent();
        writer.println("private void put(final String serviceName) { ");
        writer.indent();
        writer.println("put(serviceName,new " + webserviceCreatorClass + "()  {");
        writer.indent();
        writer.println("@Override public Object create(HttpServletRequest request, HttpServletResponse response) {");
        writer.indent();
        writer.println(requestModuleName + " requestModule = new " + requestModuleName + "(request, response);");
        writer.println("switch (serviceName) {");
        writer.indent();
        for (TypeElement method : remoteMethods)
        {
            String interfaceName = method.getQualifiedName().toString();
            String methodName = toJavaName(method).toLowerCase();
            writer.println("case \"" + interfaceName + "\": return component." + methodName + "(requestModule).get();");
        }
        writer.println("default: return null;");
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("});");
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("}");

    }

    private SourceWriter createSourceWriter(String packageName, String artifactId, String type) throws IOException
    {
        final Filer filer = processingEnvironment.getFiler();
        final String moduleListFile = "org.rapla.Dagger" + type + "Module";
        final String fullModuleName = packageName + ".Dagger" + artifactId + type + "Module";
        final String fullModuleStarterName = packageName + ".Dagger" + artifactId + type + "StartupModule";
        if (type != null && !type.isEmpty())
        {
            final File allserviceList = AnnotationInjectionProcessor.getFile(processingEnvironment.getFiler());
            AnnotationInjectionProcessor.addServiceFile(moduleListFile, allserviceList, fullModuleName);

            // look for StarterModulse
            if (processingEnvironment.getElementUtils().getTypeElement(fullModuleStarterName) != null)
            {
                AnnotationInjectionProcessor.addServiceFile(moduleListFile, allserviceList, fullModuleStarterName);
            }
        }
        final JavaFileObject source = filer.createSourceFile(fullModuleName);
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
        moduleWriter.println("public class Dagger" + artifactId + type + "Module {");
        moduleWriter.indent();
        return moduleWriter;
    }

    private SourceWriter createWebserviceComponentSourceWriter(String packageName, String artifcatName) throws IOException
    {
        String type = artifcatName + "Webservice";
        final String componentName = getComponentName(packageName, type);
        final Filer filer = processingEnvironment.getFiler();
        {// serviceFile filling
            final String serviceFileName = "org.rapla.DaggerWebserviceComponents";
            final File allserviceList = AnnotationInjectionProcessor.getFile(processingEnvironment.getFiler());
            AnnotationInjectionProcessor.addServiceFile(serviceFileName, allserviceList, componentName);
        }
        final JavaFileObject source = filer.createSourceFile(componentName);
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package " + packageName + ";");
        moduleWriter.println();
        moduleWriter.println("import " + HttpServletRequest.class.getCanonicalName() + ";");
        moduleWriter.println("import " + HttpServletResponse.class.getCanonicalName() + ";");
        moduleWriter.println("import " + BasicRequestModule.class.getCanonicalName() + ";");
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
        //String basicRequestModuleName = BasicRequestModule.class.getSimpleName();
        moduleWriter.println("@Subcomponent");
        moduleWriter.println("public interface Dagger" + type + "Component {");
        moduleWriter.indent();
        return moduleWriter;
    }

    private String getComponentName(String packageName, String type)
    {
        return packageName + ".Dagger" + type + "Component";
    }

    BitSet createMethods(String interfaceName, File allserviceList, String artifactName) throws Exception
    {
        BitSet exportedInterface = new BitSet();
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
                        final BitSet exported = generateDefaultImplementation(artifactName, implementingClassTypeElement, interfaceClassTypeElement,
                                defaultImplementation);
                        exportedInterface.or(exported);
                    }
                }
                final DefaultImplementation defaultImplementation = implementingClassTypeElement.getAnnotation(DefaultImplementation.class);
                if (defaultImplementation != null)
                {
                    final BitSet exported = generateDefaultImplementation(artifactName, implementingClassTypeElement, interfaceClassTypeElement,
                            defaultImplementation);
                    exportedInterface.or(exported);
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
        return exportedInterface;
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
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnServer(context))
        {
            if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[SERVER_SOURCE_WRITER];
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnSwing(context))
        {
            if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
            {
                alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
                SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }

    }

    private void generateEmptyExtensionMethods(final TypeElement interfaceName, SourceWriter moduleWriter)
    {
        moduleWriter.println();
        //        if (isSingleton)
        //        {
        //            final String fullQualifiedName = "provide_" + interfaceNameWithoutPackage + "_" + defaultImplClassName;
        //            moduleWriter.println("private " + qualifiedName + " " + fullQualifiedName + "=null;");
        //        }
        writeEmptyMethod(interfaceName, moduleWriter);
    }

    private void writeEmptyMethod(TypeElement interfaceName, SourceWriter moduleWriter)
    {
        final String collectionTypeString;
        moduleWriter.println();
        moduleWriter.println("@Provides(type=Type.SET_VALUES)");
        collectionTypeString = "Set";
        final String fullQualifiedName = "provide_" + toJavaName(interfaceName) + "_empty";
        moduleWriter.println(
                "public java.util.Set<" + interfaceName.getQualifiedName().toString() + "> " + fullQualifiedName + "_" + collectionTypeString + "() {");
        moduleWriter.indent();
        moduleWriter.println("return java.util.Collections.emptySet();");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private BitSet generateDefaultImplementation(String artifactName, TypeElement implementingClassTypeElement, TypeElement interfaceTypeElement,
            DefaultImplementation defaultImplementation)
    {
        if (!getDefaultImplementationOf(defaultImplementation).equals(interfaceTypeElement))
        {
            return new BitSet();
        }
        BitSet exportedInterface = new BitSet();
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
                if (defaultImplementation.export())
                {
                    exportedInterface.set(GWT_SOURCE_WRITER);
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
                        generateWebserviceComponent(artifactName, implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    }
                }
                if (!alreadyGenerated[SERVER_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[SERVER_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[requestScoped ? REQUEST_SOURCE_WRITER : SERVER_SOURCE_WRITER];
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    if (defaultImplementation.export())
                    {
                        exportedInterface.set(SERVER_SOURCE_WRITER);
                    }
                }
            }
            if (InjectionContext.isInjectableOnSwing(context))
            {
                if (!alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].contains(generated))
                {
                    alreadyGenerated[JAVA_CLIENT_SOURCE_WRITER].add(generated);
                    SourceWriter moduleWriter = sourceWriters[JAVA_CLIENT_SOURCE_WRITER];
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    if (defaultImplementation.export())
                    {
                        exportedInterface.set(JAVA_CLIENT_SOURCE_WRITER);
                    }
                }
            }

        }
        return exportedInterface;
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
    private void generateWebserviceComponent(String artifactName, TypeElement implementingClassTypeElement, TypeElement interfaceName,
            SourceWriter moduleWriter)
    {
        remoteMethods.add(interfaceName);

        //final String simpleName = interfaceName.getQualifiedName().toString();
        final String methodName = toJavaName(interfaceName).toLowerCase();
        // it is important that the first char is a Uppercase latter otherwise dagger fails with IllegalArgumentException
        final String serviceComponentName = firstCharUp(toJavaName(interfaceName)) + "Service";
        //final String daggerRequestModuleName = "Dagger" + artifactName + "RequestModule";
        String basicRequestModuleName = BasicRequestModule.class.getSimpleName();
        String implementingName = implementingClassTypeElement.getQualifiedName().toString();
        Set<String> requestModuleList = new LinkedHashSet<String>();
        requestModuleList.add(basicRequestModuleName);
        final String daggerRequestModuleName = "Dagger" + artifactName + "RequestModule";
        requestModuleList.add(daggerRequestModuleName);

        moduleWriter.println();
        moduleWriter.println(serviceComponentName + " " + methodName + "(" + basicRequestModuleName + " module);");
        moduleWriter.print("@RequestScoped @Subcomponent(modules={");
        boolean first = true;
        for (String name : requestModuleList)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                moduleWriter.print(",");
            }
            moduleWriter.print(name + ".class");
        }
        moduleWriter.println("})");
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
            processingEnvironment.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "No requestscope allowed on Extensions but used in " + implementingClassTypeElement.toString());
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
