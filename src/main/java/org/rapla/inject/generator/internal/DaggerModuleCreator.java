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
import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.generator.internal.GwtProxyCreator;
import org.rapla.jsonrpc.generator.internal.JavaClientProxyCreator;
import org.rapla.jsonrpc.server.WebserviceCreator;
import org.rapla.jsonrpc.server.WebserviceCreatorMap;
import org.rapla.server.RequestScoped;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.ws.rs.Path;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
    enum Scopes
    {
        Server("Server","server.dagger"),
        JavaClient( "JavaClient","client.swing.dagger"),
        Gwt("Gwt","client.gwt.dagger"),
        Webservice("Webservice","server.dagger"),
        WebserviceModule("WebserviceComponent", "server.dagger"),
        Request("Request","server.dagger");

        final String packageNameSuffix;
        final String name;
        Scopes(String name,String packageNameSuffix)
        {
            this.name = name;this.packageNameSuffix = packageNameSuffix;
        }

        @Override public String toString()
        {
            return name;
        }

        public String getPackageName(String originalPackageName)
        {
            final String packageNameWithPoint = originalPackageName + (originalPackageName.length() == 0 ? "" : ".") ;
            final String packageName = packageNameWithPoint + packageNameSuffix;
            return  packageName;
        }
    }

    private SourceWriter getWriter(Scopes scope)
    {
        return sourceWriters.get(scope);
    }

    private SourceWriter getWriter(Scopes scope,Generated generated)
    {
        Set<Generated> generatedSet = alreadyGenerated.get(scope);
        if ( generatedSet == null)
        {
            generatedSet = new HashSet<>();
            alreadyGenerated.put( scope,generatedSet);
        }
        generatedSet.add(generated);
        return getWriter( scope);
    }

    private final Map<Scopes,Set<Generated>> alreadyGenerated = new LinkedHashMap<>();
    private final Map<Scopes,SourceWriter> sourceWriters = new LinkedHashMap<>();
    private final ProcessingEnvironment processingEnvironment;
    private final String generatorName;

    public DaggerModuleCreator(ProcessingEnvironment processingEnvironment, String generatorName)
    {
        super();
        this.generatorName = generatorName;
        this.processingEnvironment = processingEnvironment;
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

    /**
     * Reads the interfaces defined in META-INF/org.rapla.servicelist into the provided set
     * and returns the File.
     */
    private void generateModuleClass( String moduleName) throws Exception
    {
        final int i = moduleName.lastIndexOf(".");
        final String packageName = (i >= 0 ? moduleName.substring(0, i) : "");
        String artifactName = firstCharUp(i >= 0 ? moduleName.substring(i + 1) : moduleName);
        remoteMethods.clear();
        createSourceWriter(packageName, artifactName, Scopes.Server);
        createSourceWriter(packageName, artifactName, Scopes.JavaClient);
        createSourceWriter(packageName, artifactName, Scopes.Gwt);
        createSourceWriter(packageName, artifactName, Scopes.WebserviceModule);
        createWebserviceComponentSourceWriter(packageName, artifactName,Scopes.Webservice);
        createSourceWriter(packageName, artifactName, Scopes.Request);

        File f = AnnotationInjectionProcessor.getInterfaceList(processingEnvironment.getFiler());
        List<String> interfaces = AnnotationInjectionProcessor.readLines( f);
        for (String interfaceName : interfaces)
        {
            createMethods(interfaceName, artifactName);
        }
        writeWebserviceList(packageName,artifactName);
        for (SourceWriter moduleWriter : sourceWriters.values())
        {
            moduleWriter.outdent();
            moduleWriter.println("}");
            moduleWriter.close();
        }
        generateComponents(packageName, artifactName);
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
     * @param packageName
     * @param artifactName
     */
    private void generateComponents(String packageName, String artifactName) throws Exception
    {
        Map<String, BitSet> exportedInterfaces = new LinkedHashMap<>();
        for ( Scopes scope: Scopes.values())
        {
            String fileName = "META-INF/"+ getExportListFilename(scope);
            final Set<String> exportInterfaces = loadLinesFromMetaInfo(fileName);
            for ( String interfaceName:exportInterfaces)
            {
                BitSet bitSet = exportedInterfaces.get(interfaceName);
                if ( bitSet == null)
                {
                    bitSet = new BitSet();
                    exportedInterfaces.put(interfaceName,bitSet);
                }
                int bit = scope.ordinal();
                bitSet.set(bit);
            }
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName , artifactName, Scopes.Server);
            String componentName = artifactName + "Server" + "Component";
            {// create dagger webservice components
                final String file = "META-INF/" + getModuleListFileName(Scopes.Webservice);
                final Set<String> webserviceComponents = loadLinesFromMetaInfo(file);
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
                //                @Singleton public static class ServiceMap extends LinkedHashMap<String,WebserviceCreator> {
                //                    @Inject public ServiceMap(ServerComponent component) {
                //                        putAll(component.getWebservice().getMap());
                //                        putAll(component.getWebservice2().getMap());
                //                    }
                //                }
                String webserviceCreatorClass = WebserviceCreator.class.getCanonicalName();
                String webserviceCreatorMapClass = WebserviceCreatorMap.class.getCanonicalName();
                sourceWriter.println(
                        "@Singleton public static class ServiceMap extends java.util.LinkedHashMap<String," + webserviceCreatorClass + ">  implements "
                                + webserviceCreatorMapClass + " {");
                sourceWriter.indent();
                sourceWriter.println("@Inject public ServiceMap(" + componentName + " component) {");
                sourceWriter.indent();

                for (int i = 0; i < size; i++)
                {
                    sourceWriter.println("putAll(component.getWebservices" + i + "().getMap());");
                }
                sourceWriter.outdent();
                sourceWriter.println("}");
                sourceWriter.println("public " + webserviceCreatorClass + " get(String serviceName) {return super.get( serviceName); }");
                sourceWriter.println(
                        "public java.util.Map<String," + webserviceCreatorClass + "> asMap() {return java.util.Collections.unmodifiableMap(this); }");
                sourceWriter.outdent();
                sourceWriter.println("}");

            }
            printExported(exportedInterfaces, sourceWriter, Scopes.Server);
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName, artifactName, Scopes.JavaClient);
            printExported(exportedInterfaces, sourceWriter, Scopes.JavaClient);
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName, artifactName, Scopes.Gwt);
            printExported(exportedInterfaces, sourceWriter, Scopes.Gwt);
            sourceWriter.outdent();
            sourceWriter.println("}");
            sourceWriter.close();
        }
    }

    private void printExported(Map<String, BitSet> exportedInterfaces, SourceWriter sourceWriter, Scopes sourceWriterIndex)
    {
        for (Map.Entry<String, BitSet> entry : exportedInterfaces.entrySet())
        {// create starter
            String key = entry.getKey();
            BitSet bitSet = entry.getValue();
            if (bitSet.get(sourceWriterIndex.ordinal()))
            {
                final int i = key.lastIndexOf(".");
                String simpleName = i > 0 ? key.substring(i + 1) : key;
                String javaname = firstCharUp(simpleName);
                sourceWriter.println(key + " get" + javaname + "();");
            }
        }
    }

    private SourceWriter createComponentSourceWriter(String originalPackageName,  String artifactId, Scopes scope) throws Exception
    {
        String packageName = scope.getPackageName( originalPackageName);
        final String file = "META-INF/" + getModuleListFileName(scope);
        final Set<String> modules = loadLinesFromMetaInfo(file);
        if ( modules.size() == 0)
        {
            final String msg = "No Module found for " + originalPackageName + artifactId + scope.toString();
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
        if ( scope == Scopes.Server)
        {
            String moduleName = "Dagger" + firstCharUp(artifactId) + Scopes.WebserviceModule.toString() + "Module";
            String webserviceComponentModule = Scopes.WebserviceModule.getPackageName(originalPackageName) + "." + moduleName;

            modules.add(webserviceComponentModule);
        }

        final String simpleComponentName = artifactId + scope.toString() + "Component";
        final String componentName = packageName + "." + simpleComponentName;

        final JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(componentName);
        final SourceWriter sourceWriter = new SourceWriter(sourceFile.openOutputStream());
        sourceWriter.println("package " + packageName + ";");
        sourceWriter.println();
        sourceWriter.println("import " + Inject.class.getCanonicalName() + ";");
        sourceWriter.println("import " + Singleton.class.getCanonicalName() + ";");
        sourceWriter.println("import " + Component.class.getCanonicalName() + ";");
        sourceWriter.println();
        sourceWriter.println(getGeneratorString());
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

    private Set<String> loadLinesFromMetaInfo(String file) throws IOException
    {
        Set<String> foundLines = new LinkedHashSet<>();
        final ClassLoader classLoader = DaggerModuleCreator.class.getClassLoader();
        final FileObject resource = processingEnvironment.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", file);
        if (resource != null && new File(resource.toUri()).exists())
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openInputStream(), "UTF-8"));)
            {
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    foundLines.add(line);
                }
            }
        }
        final Enumeration<URL> resources = classLoader.getResources(file);
        if (resource != null)
        {
            while (resources.hasMoreElements())
            {
                final URL nextElement = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(nextElement.openStream(), "UTF-8")))
                {
                    String line = null;
                    while ((line = reader.readLine()) != null)
                    {
                        foundLines.add(line);
                    }
                }
            }
        }
        return foundLines;
    }

    private void writeWebserviceList(String originalPackageName,String artifactId) throws IOException
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
        String packageName = Scopes.WebserviceModule.getPackageName( originalPackageName);
        {
            SourceWriter writer = getWriter(Scopes.WebserviceModule);
            String serviceMapClass = packageName + "." + artifactId + "ServerComponent.ServiceMap";
            writer.println("@Provides " + WebserviceCreatorMap.class.getCanonicalName() + " getWebserviceList("+serviceMapClass+" impl) { return impl;}" );
        }



        String className = "Dagger" + artifactId + "WebserviceMap";
        {
            SourceWriter writer = getWriter(Scopes.Webservice);
            writer.println(className + " getMap();");
        }



        final Filer filer = processingEnvironment.getFiler();
        String fullFilename = packageName + "." + className;
        String webserviceCreatorClass = WebserviceCreator.class.getCanonicalName();

        final JavaFileObject source = filer.createSourceFile(fullFilename);
        try (OutputStream out=source.openOutputStream())
        {
            final SourceWriter writer = new SourceWriter(out);
            writer.println("package " + packageName + ";");
            writer.println("import " + HttpServletRequest.class.getCanonicalName() + ";");
            writer.println("import " + HttpServletResponse.class.getCanonicalName() + ";");
            writer.println(getGeneratorString());
            writer.println("@javax.inject.Singleton public class "+ className + " extends java.util.LinkedHashMap<String," + webserviceCreatorClass + "> {");
            writer.indent();
            final String componentName = "Dagger" + artifactId + "WebserviceComponent";
            writer.println("final " + componentName + " component;");
            writer.println("@javax.inject.Inject "+ className + "(final " + componentName + " component) { ");
            writer.indent();
            //final String requestModuleName = "Dagger" + artifact + "RequestModule";
            final String requestModuleName = BasicRequestModule.class.getCanonicalName();
            writer.println("this.component = component;");
            for (TypeElement method : remoteMethods)
            {
                final String path = getPath(method);
                writer.println("put(\"" + path + "\");");
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
                final String path = getPath(method);
                String methodName = toJavaName(method).toLowerCase();
                writer.println("case \"" + path + "\": return component." + methodName + "(requestModule).get();");
            }
            writer.println("default: return null;");
            writer.outdent();
            writer.println("}");
            writer.outdent();
            writer.println("}");


            writer.println("@Override public Class getServiceClass() {");
            writer.indent();
            writer.println("switch (serviceName) {");
            writer.indent();
            for (TypeElement method : remoteMethods)
            {
                final String path = getPath(method);
                writer.println("case \"" + path + "\": return "+method.getQualifiedName().toString()+".class;");
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
            writer.close();
        }

    }

    private String getPath(TypeElement method)
    {
        final String path;
        final Path pathAnnotation = method.getAnnotation(Path.class);
        if ( pathAnnotation != null)
        {
            path = pathAnnotation.value();
        }
        else
        {
            path = method.getQualifiedName().toString();
        }
        return path;
    }

    void appendLineToMetaInf(String filename, String line) throws IOException
    {
        final File folder = getMetaInfFolder();
        AnnotationInjectionProcessor.appendToFile(filename, folder, line);
    }

    private void createSourceWriter(String originalPackageName, String artifactId,Scopes scope) throws IOException
    {
        String packageName = scope.getPackageName( originalPackageName);
        final Filer filer = processingEnvironment.getFiler();
        final String moduleListFile = getModuleListFileName(scope);
        final String fullModuleName = packageName + ".Dagger" + artifactId + scope + "Module";
        final String fullModuleStarterName = packageName + ".Dagger" + artifactId + scope + "StartupModule";
        //if (type != null && !type.isEmpty())
        {
            appendLineToMetaInf(moduleListFile, fullModuleName);
            // look for a Files ending with StarterModule and beginning with the daggermodule name
            // This files allow custom DaggerModuleCustomization
            if (processingEnvironment.getElementUtils().getTypeElement(fullModuleStarterName) != null)
            {
                appendLineToMetaInf(moduleListFile, fullModuleStarterName);
            }
        }
        final JavaFileObject source = filer.createSourceFile(fullModuleName);
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package " + packageName + ";");
        moduleWriter.println();
        if (scope == Scopes.Request)
        {
            moduleWriter.println("import " + HttpServletRequest.class.getCanonicalName() + ";");
            moduleWriter.println("import " + HttpServletResponse.class.getCanonicalName() + ";");
        }
        moduleWriter.println("import " + Provides.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provides.Type.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Module.class.getCanonicalName() + ";");
        moduleWriter.println("import " + DaggerMapKey.class.getCanonicalName() + ";");
        moduleWriter.println();
        moduleWriter.println(getGeneratorString());
        moduleWriter.println("@Module");
        moduleWriter.println("public class Dagger" + artifactId + scope + "Module {");
        moduleWriter.indent();

        sourceWriters.put(scope, moduleWriter);
    }

    private String getModuleListFileName(Scopes scope)
    {
        String suffix = ( scope == Scopes.Webservice) ? "Components":"Modules";
        return "org.rapla.Dagger" + scope + suffix;
    }

    private String getExportListFilename(Scopes scope)
    {
        return "org.rapla.Dagger" + scope + "Exported";
    }

    private void createWebserviceComponentSourceWriter(String originalPackageName, String artifactId, Scopes scope) throws IOException
    {
        String packageName = scope.getPackageName( originalPackageName);
        String type = scope.toString();
        final String simpleComponentName = "Dagger" + artifactId + type + "Component";
        final String fullComponentName = packageName + "." + simpleComponentName;
        final Filer filer = processingEnvironment.getFiler();
        {// serviceFile filling
            appendLineToMetaInf(getModuleListFileName(Scopes.Webservice),  fullComponentName);
        }
        final JavaFileObject source = filer.createSourceFile(fullComponentName);
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package " + packageName + ";");
        moduleWriter.println();
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
        moduleWriter.println("public interface " +simpleComponentName + " {");
        moduleWriter.indent();
        sourceWriters.put(scope, moduleWriter);
    }

    private File getMetaInfFolder() throws IOException
    {
        return AnnotationInjectionProcessor.getInterfaceList(processingEnvironment.getFiler()).getParentFile();
    }

    void createMethods(String interfaceName, String artifactName) throws Exception
    {
        BitSet exportedInterface = new BitSet();
        File folder = getMetaInfFolder();
        final List<String> implementingClasses = AnnotationInjectionProcessor.readLines( AnnotationInjectionProcessor.getFile(folder,"services/" + interfaceName));
        final TypeElement interfaceClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(interfaceName);
        for (String implementingClass : implementingClasses)
        {
            final TypeElement implementingClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(implementingClass);
            if (implementingClass.endsWith(GwtProxyCreator.PROXY_SUFFIX))
            {
                generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, Scopes.Gwt);
            }
            else if (implementingClass.endsWith(JavaClientProxyCreator.PROXY_SUFFIX))
            {
                generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, Scopes.JavaClient);
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
                final Path path = implementingClassTypeElement.getAnnotation(Path.class);
                if(path != null)
                {
                    TypeElement interfaceTypeElement = implementingClassTypeElement;
                    final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(), implementingClassTypeElement.getQualifiedName().toString());
                    if ( notGenerated( Scopes.Webservice, generated))
                    {
                        SourceWriter moduleWriter = getWriter(Scopes.Webservice, generated);
                        generateWebserviceComponent(artifactName, implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    }
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
        for (Scopes scope: Scopes.values())
        {
            if ( exportedInterface.get(scope.ordinal()))
            {
                final String filename = getExportListFilename(scope);
                appendLineToMetaInf(filename, interfaceName);
            }
        }
    }



    /*
    private void generatePath(TypeElement implementingClassTypeElement, String path)
    {
        final String qualifiedName = implementingClassTypeElement.getQualifiedName().toString();
        final String qualifiedRestPage = qualifiedName.replaceAll("\\.", "_");
        final Generated generated = new Generated("RestPage", qualifiedRestPage);
        if(notGenerated(Scopes.Server, generated))
        {
            if(path.contains("/"))
            {
                path = path.substring(0, path.indexOf("/"));
            }
            final SourceWriter writer = getWriter(Scopes.Server, generated);
            writer.println("@Provides(type=Type.MAP) @javax.inject.Singleton");
            writer.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + path + "\")");
            final ExecutableElement constructor = getConstructor(implementingClassTypeElement);
            final List<? extends VariableElement> parameters = constructor.getParameters();
            final String factoryName = qualifiedName + "_Factory";
            boolean hasMemberInjector = implementingClassTypeElement.getSuperclass() != null;
            writer.print("public " + Factory.class.getCanonicalName() + " provide_Rest_" + qualifiedRestPage + "_Factory(");
            if (hasMemberInjector)
            {
                writer.print(MembersInjector.class.getCanonicalName() + "<" + qualifiedName + "> members, ");
            }
            writer.println(createString(parameters, true) + ") {");
            writer.indent();
            writer.print("return " + factoryName + ".create(");
            if (hasMemberInjector)
            {
                writer.print("members, ");
            }
            writer.println(createString(parameters, false) + ");");
            writer.outdent();
            writer.println("}");
        }
    }*/

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    private void generateProxyMethods(String interfaceName, String implementingClass, TypeElement interfaceClassTypeElement, Scopes sourceWriterIndex)
    {
        final Generated generated = new Generated(interfaceName, implementingClass);
        if (notGenerated(sourceWriterIndex, generated))
        {
            final SourceWriter sourceWriter = getWriter(sourceWriterIndex, generated);
            sourceWriter.println();
            sourceWriter.println("@Provides");
            sourceWriter.println(
                    "public " + interfaceName + " provide_" + toJavaName(interfaceClassTypeElement) + "_" + implementingClass.replaceAll("\\.", "_") + "("
                            + implementingClass + " result) {");
            sourceWriter.indent();
            sourceWriter.println("return result;");
            sourceWriter.outdent();
            sourceWriter.println("}");
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

    private boolean notGenerated(Scopes scope, Generated generated)
    {
        final boolean isGenerated;
        final Set<Generated> generatedSet = alreadyGenerated.get(scope);
        if ( generatedSet == null)
        {
            isGenerated = false;
        }
        else
        {
            isGenerated = generatedSet.contains(generated);
        }
        return !isGenerated;
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
            if (notGenerated(Scopes.Gwt, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Gwt, generated);
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnServer(context))
        {
            if (notGenerated(Scopes.Server, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Server, generated);
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnSwing(context))
        {
            if (notGenerated(Scopes.JavaClient, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.JavaClient, generated);
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
        moduleWriter.println("@javax.inject.Singleton @Provides(type=Type.SET_VALUES)");
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
                if (notGenerated(Scopes.Gwt, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.Gwt, generated);
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                }
                if (defaultImplementation.export())
                {
                    exportedInterface.set(Scopes.Gwt.ordinal());
                }
            }
            if (InjectionContext.isInjectableOnServer(context))
            {
                if (interfaceTypeElement.getAnnotation(RemoteJsonMethod.class) != null)
                {
                    if (notGenerated(Scopes.Webservice,generated))
                    {
                        SourceWriter moduleWriter = getWriter(Scopes.Webservice, generated);
                        generateWebserviceComponent(artifactName, implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    }
                }
                if (notGenerated(Scopes.Server, generated))
                {
                    SourceWriter moduleWriter = getWriter(requestScoped ? Scopes.Request : Scopes.Server, generated);
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter, requestScoped ? RequestScoped.class.getCanonicalName() : null);
                    if (defaultImplementation.export())
                    {
                        exportedInterface.set(Scopes.Server.ordinal());
                    }
                }
            }
            if (InjectionContext.isInjectableOnSwing(context))
            {
                if (notGenerated(Scopes.JavaClient, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.JavaClient, generated);
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    if (defaultImplementation.export())
                    {
                        exportedInterface.set(Scopes.JavaClient.ordinal());
                    }
                }
            }

        }
        return exportedInterface;
    }

    private void generateDefaultImplementation(TypeElement implementingClassTypeElement, TypeElement interfaceName, SourceWriter moduleWriter)
    {
        generateDefaultImplementation(implementingClassTypeElement,interfaceName,moduleWriter, null);
    }
    private void generateDefaultImplementation(TypeElement implementingClassTypeElement, TypeElement interfaceName, SourceWriter moduleWriter,String scopeAnnotation)
    {
        moduleWriter.println();
        if (scopeAnnotation != null)
        {
            moduleWriter.println("@"+scopeAnnotation);
        }
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
            if (notGenerated(Scopes.Gwt, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Gwt,generated);
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnServer(context))
        {
            if (notGenerated(Scopes.Server, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Server,generated);
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        if (InjectionContext.isInjectableOnSwing(context))
        {
            if (notGenerated(Scopes.JavaClient, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.JavaClient, generated);
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
            moduleWriter.println("@Provides(type=Type.MAP) @javax.inject.Singleton");
            moduleWriter.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + extension.id() + "\")");
            final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = id + "_Map";
        }
        else
        {
            moduleWriter.println();
            moduleWriter.println("@Provides(type=Type.SET) @javax.inject.Singleton");
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
    
    private String createString(List<? extends VariableElement> parameters, boolean withType)
    {
        final StringBuilder sb = new StringBuilder();
        if (parameters != null)
        {
            boolean first = true;
            for (VariableElement parameter : parameters)
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
                    sb.append(Provider.class.getCanonicalName()+"<");
                    final TypeMirror asType = parameter.asType();
                    if (asType instanceof PrimitiveType)
                    {
                        final String string = asType.toString();
                        if ("int".equalsIgnoreCase(string))
                        {
                            sb.append("java.lang.Integer");
                        }
                        else
                        {
                            sb.append("java.lang.");
                            final char[] charArray = string.toCharArray();
                            charArray[0] = Character.toUpperCase(charArray[0]);
                            sb.append(charArray);
                        }
                    }
                    else
                    {
                        sb.append(asType.toString());
                    }
                    sb.append("> ");
                }
                sb.append(parameter.getSimpleName().toString());
            }
        }
        return sb.toString();
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
                if (hasInjectAnnotation)
                {
                    foundConstructor = constructor;
                    break;
                }
            }
        }
        return foundConstructor;
    }
}
