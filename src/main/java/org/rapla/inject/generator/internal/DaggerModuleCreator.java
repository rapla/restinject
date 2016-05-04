package org.rapla.inject.generator.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.ws.rs.Path;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.generator.AnnotationInjectionProcessor;
import org.rapla.inject.internal.DaggerMapKey;
import org.rapla.rest.generator.internal.GwtProxyCreator;
import org.rapla.rest.generator.internal.JavaClientProxyCreator;

import dagger.Component;
import dagger.MembersInjector;
import dagger.Module;
import dagger.Provides;

public class DaggerModuleCreator
{

    private static class Generated
    {
        private final String interfaceName;
        private final String className;
        private final String id;

        public Generated(String interfaceName, String className, String id)
        {
            super();
            this.interfaceName = interfaceName;
            this.className = className;
            this.id = id;
        }

        @Override public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
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
            if (id == null)
            {
                if (other.id != null)
                    return false;
            }
            else if (!id.equals(other.id))
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

    enum Scopes
    {
        Common("Common", "common.dagger"),
        Server("Server", "server.dagger"),
        Client("Client", "client.dagger"),
        JavaClient("JavaClient", "client.swing.dagger"),
        Gwt("Gwt", "client.gwt.dagger");
        //        Webservice("Webservice", "server.dagger"),
        //        WebserviceModule("WebserviceComponent", "server.dagger");

        final String packageNameSuffix;
        final String name;

        static Scopes[] components()
        {
            return new Scopes[] { Server, JavaClient, Gwt };
        }

        Scopes(String name, String packageNameSuffix)
        {
            this.name = name;
            this.packageNameSuffix = packageNameSuffix;
        }

        @Override public String toString()
        {
            return name;
        }

        public String getPackageName(String originalPackageName)
        {
            final String packageNameWithPoint = originalPackageName + (originalPackageName.length() == 0 ? "" : ".");
            final String packageName = packageNameWithPoint + packageNameSuffix;
            return packageName;
        }
    }

    private SourceWriter getWriter(Scopes scope)
    {
        return sourceWriters.get(scope);
    }

    private SourceWriter getWriter(Scopes scope, Generated generated)
    {
        Set<Generated> generatedSet = alreadyGenerated.get(scope);
        if (generatedSet == null)
        {
            generatedSet = new HashSet<>();
            alreadyGenerated.put(scope, generatedSet);
        }
        generatedSet.add(generated);
        return getWriter(scope);
    }

    private final Map<Scopes, Set<Generated>> alreadyGenerated = new LinkedHashMap<>();
    private final Map<Scopes, SourceWriter> sourceWriters = new LinkedHashMap<>();
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
        final long start = System.currentTimeMillis();
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Module " + moduleName);
        generateModuleClass(moduleName);
        final long ms = System.currentTimeMillis() - start;
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "Finished generating Module " + moduleName + " took " + ms + " ms");
    }

    /**
     * Reads the interfaces defined in META-INF/org.rapla.servicelist into the provided set
     * and returns the File.
     */
    private void generateModuleClass(String moduleName) throws Exception
    {
        final int i = moduleName.lastIndexOf(".");
        final String packageName = (i >= 0 ? moduleName.substring(0, i) : "");
        String artifactName = firstCharUp(i >= 0 ? moduleName.substring(i + 1) : moduleName);
        createSourceWriter(packageName, artifactName, Scopes.Common);
        createSourceWriter(packageName, artifactName, Scopes.Server);
        createSourceWriter(packageName, artifactName, Scopes.Client);
        createSourceWriter(packageName, artifactName, Scopes.JavaClient);
        createSourceWriter(packageName, artifactName, Scopes.Gwt);

        File f = AnnotationInjectionProcessor.getInterfaceList(processingEnvironment.getFiler());
        List<String> interfaces = AnnotationInjectionProcessor.readLines(f);
        for (String interfaceName : interfaces)
        {
            createMethods(interfaceName, artifactName);
        }
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
     * import org.rapla.server.internal.ServerServiceImpl;</br></code>
     * <code>@Singleton @dagger.Component(modules = { DaggerServerModule.class, MyModule.class })</br>
     * public interface ServerComponent</br>
     * {</br>
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
        for (Scopes scope : Scopes.components())
        {
            String fileName = "META-INF/" + getExportListFilename(scope);
            final Set<String> exportInterfaces = loadLinesFromMetaInfo(fileName);
            for (String interfaceName : exportInterfaces)
            {
                BitSet bitSet = exportedInterfaces.get(interfaceName);
                if (bitSet == null)
                {
                    bitSet = new BitSet();
                    exportedInterfaces.put(interfaceName, bitSet);
                }
                int bit = scope.ordinal();
                bitSet.set(bit);
            }
        }
        {
            SourceWriter sourceWriter = createComponentSourceWriter(packageName, artifactName, Scopes.Server);
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

    private SourceWriter createComponentSourceWriter(String originalPackageName, String artifactId, Scopes scope) throws Exception
    {
        String packageName = scope.getPackageName(originalPackageName);
        final Set<String> modules = new HashSet<>();
        //        {
        //            final String file = "META-INF/" + getModuleListFileName(Scopes.Common);
        //            modules.addAll(loadLinesFromMetaInfo(file));
        //        }
        //        if ( scope == Scopes.Gwt || scope == Scopes.JavaClient)
        //        {
        //            final String file = "META-INF/" + getModuleListFileName(Scopes.Client);
        //            modules.addAll(loadLinesFromMetaInfo(file));
        //        }
        {
            final String file = "META-INF/" + getModuleListFileName(scope);
            modules.addAll(loadLinesFromMetaInfo(file));
        }

        if (modules.size() == 0)
        {
            final String msg = "No Module found for " + originalPackageName + artifactId + scope.toString();
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
        //        if (scope == Scopes.Server)
        //        {
        //            String moduleName = "Dagger" + firstCharUp(artifactId) + Scopes.WebserviceModule.toString() + "Module";
        //            String webserviceComponentModule = Scopes.WebserviceModule.getPackageName(originalPackageName) + "." + moduleName;
        //
        //            modules.add(webserviceComponentModule);
        //        }

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

    void appendLineToMetaInf(String filename, String line) throws IOException
    {
        final File folder = getMetaInfFolder();
        AnnotationInjectionProcessor.appendToFile(filename, folder, line);
    }

    private void createSourceWriter(String originalPackageName, String artifactId, Scopes scope) throws IOException
    {
        String packageName = scope.getPackageName(originalPackageName);
        final Filer filer = processingEnvironment.getFiler();
        final String moduleListFile = getModuleListFileName(scope);
        final String fullModuleName = getFullModuleName(originalPackageName, artifactId, scope) + "Module";
        //if (type != null && !type.isEmpty())
        {
            appendLineToMetaInf(moduleListFile, fullModuleName);
            // look for a Files ending with StarterModule and beginning with the daggermodule name
            // This files allow custom DaggerModuleCustomization
            final String fullModuleStarterName = getFullModuleName(originalPackageName, artifactId, scope) + "StartupModule";
            if (processingEnvironment.getElementUtils().getTypeElement(fullModuleStarterName) != null)
            {
                appendLineToMetaInf(moduleListFile, fullModuleStarterName);
            }
        }
        final JavaFileObject source = filer.createSourceFile(fullModuleName);
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package " + packageName + ";");
        moduleWriter.println();
        moduleWriter.println("import " + Provides.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Provides.Type.class.getCanonicalName() + ";");
        moduleWriter.println("import " + Module.class.getCanonicalName() + ";");
        moduleWriter.println("import " + DaggerMapKey.class.getCanonicalName() + ";");
        moduleWriter.println();
        moduleWriter.println(getGeneratorString());
        moduleWriter.print("@Module(includes={");
        final String commonModuleName = getFullModuleName(originalPackageName, artifactId, Scopes.Common) + "Module.class";
        final String clientModuleName = getFullModuleName(originalPackageName, artifactId, Scopes.Client) + "Module.class";
        if (scope == Scopes.Server || scope == Scopes.Client)
        {
            moduleWriter.print(commonModuleName);
        }
        else if ( scope != Scopes.Common)
        {
            moduleWriter.print( clientModuleName);
        }
        moduleWriter.println("})");
        moduleWriter.println("public class Dagger" + artifactId + scope + "Module {");
        moduleWriter.indent();

        sourceWriters.put(scope, moduleWriter);
    }

    private String getFullModuleName(String originalPackageName, String artifactId, Scopes scope)
    {
        return scope.getPackageName(originalPackageName) + ".Dagger" + artifactId + scope;
    }

    private String getModuleListFileName(Scopes scope)
    {
        String suffix = /*(scope == Scopes.Webservice) ? "Components" : */"Modules";
        return "org.rapla.Dagger" + scope + suffix;
    }

    private String getExportListFilename(Scopes scope)
    {
        return "org.rapla.Dagger" + scope + "Exported";
    }

    private File getMetaInfFolder() throws IOException
    {
        return AnnotationInjectionProcessor.getInterfaceList(processingEnvironment.getFiler()).getParentFile();
    }

    void createMethods(String interfaceName, String artifactName) throws Exception
    {
        BitSet exportedInterface = new BitSet();
        File folder = getMetaInfFolder();
        final List<String> implementingClasses = AnnotationInjectionProcessor
                .readLines(AnnotationInjectionProcessor.getFile(folder, "services/" + interfaceName));
        interfaceName = interfaceName.replaceAll("\\$", ".");
        final TypeElement interfaceClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(interfaceName);

        for (String implementingClass : implementingClasses)
        {
            implementingClass = implementingClass.replaceAll("\\$", ".");
            final TypeElement implementingClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(implementingClass);
            final Path path = implementingClassTypeElement != null ? implementingClassTypeElement.getAnnotation(Path.class) : null;
            if (interfaceClassTypeElement == null && path == null)
            {
                processingEnvironment.getMessager()
                        .printMessage(Diagnostic.Kind.ERROR, "No interface class found for " + interfaceName, implementingClassTypeElement);
                break;
            }
            else
            {
                if (implementingClass.endsWith(GwtProxyCreator.PROXY_SUFFIX))
                {
                    generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, Scopes.Gwt, "gwt");
                }
                else if (implementingClass.endsWith(JavaClientProxyCreator.PROXY_SUFFIX))
                {
                    generateProxyMethods(interfaceName, implementingClass, interfaceClassTypeElement, Scopes.JavaClient, "javaClient");
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
                    if (implementingClassTypeElement.getAnnotation(Path.class) != null)
                    {
                        final TypeElement implClass = implementingClassTypeElement;
                        final Generated generated = new Generated(implementingClassTypeElement.getQualifiedName().toString(),
                                implClass.getQualifiedName().toString(), implementingClassTypeElement.getAnnotation(Path.class).value());
                        if (notGenerated(Scopes.Server, generated))
                        {
                            SourceWriter moduleWriter = getWriter(Scopes.Server, generated);
                            generateWebserviceComponent(artifactName, implClass, implementingClassTypeElement, moduleWriter);
                        }
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
        for (Scopes scope : Scopes.values())
        {
            if (exportedInterface.get(scope.ordinal()))
            {
                final String filename = getExportListFilename(scope);
                appendLineToMetaInf(filename, interfaceName);
            }
        }
    }

    private String getGeneratorString()
    {
        return "@javax.annotation.Generated(\"" + generatorName + "\")";
    }

    private void generateProxyMethods(String interfaceName, String implementingClass, TypeElement interfaceClassTypeElement, Scopes sourceWriterIndex,
            String id)
    {
        final Generated generated = new Generated(interfaceName, implementingClass, id);
        if (notGenerated(sourceWriterIndex, generated))
        {
            final SourceWriter sourceWriter = getWriter(sourceWriterIndex, generated);
            sourceWriter.println();
            sourceWriter.println("@Provides");
            final String methodName = createMethodName(sourceWriter, interfaceClassTypeElement);
            sourceWriter.println("public " + interfaceName + " " + methodName + "(" + implementingClass + " result) {");
            sourceWriter.indent();
            sourceWriter.println("return result;");
            sourceWriter.outdent();
            sourceWriter.println("}");
        }
    }

    private String toJavaName(TypeElement className)
    {
        final Name qualifiedName = className.getQualifiedName();
        final String s = qualifiedName.toString().replaceAll("\\.", "_");
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
        if (generatedSet == null)
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
        final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(), "emtpy", annotation.id());
        if (InjectionContext.isInjectableEverywhere(context))
        {
            if (notGenerated(Scopes.Common, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Common, generated);
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        else if (InjectionContext.isInjectableOnClient(context))
        {
            if (notGenerated(Scopes.Client, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Client, generated);
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        else
        {
            if (InjectionContext.isInjectableOnServer(context))
            {
                if (notGenerated(Scopes.Server, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.Server, generated);
                    generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnGwt(context))
            {
                if (notGenerated(Scopes.Gwt, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.Gwt, generated);
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

    }

    private void generateEmptyExtensionMethods(final TypeElement interfaceName, SourceWriter moduleWriter)
    {
        moduleWriter.println();
        final String collectionTypeString;
        moduleWriter.println();
        moduleWriter.println("@javax.inject.Singleton @Provides(type=Type.SET_VALUES)");
        collectionTypeString = "Set";
        final String methodName = createMethodName(moduleWriter, interfaceName) + "_empty";
        moduleWriter.println("public java.util.Set<" + interfaceName.getQualifiedName().toString() + "> " + methodName + "_" + collectionTypeString + "() {");
        moduleWriter.indent();
        moduleWriter.println("return java.util.Collections.emptySet();");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private BitSet generateDefaultImplementation(final String artifactName, final TypeElement implementingClassTypeElement,
            final TypeElement interfaceTypeElement, final DefaultImplementation defaultImplementation)
    {
        final String id = "DefaultImplementation";
        if (interfaceTypeElement.getQualifiedName().toString().equals(Path.class.getCanonicalName()))
        {
            final TypeElement implClass = implementingClassTypeElement;
            final Generated generated = new Generated(implementingClassTypeElement.getQualifiedName().toString(), implClass.getQualifiedName().toString(), id);
            if (notGenerated(Scopes.Server, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Server, generated);
                generateWebserviceComponent(artifactName, implClass, implementingClassTypeElement, moduleWriter);
            }
        }
        if (!getDefaultImplementationOf(defaultImplementation).equals(interfaceTypeElement))
        {
            return new BitSet();
        }
        BitSet exportedInterface = new BitSet();

        //final Set<InjectionContext> context = new HashSet<>(Arrays.asList(defaultImplementation.context());
        final InjectionContext[] context = defaultImplementation.context();
        final Types typeUtils = processingEnvironment.getTypeUtils();
        final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(),
                implementingClassTypeElement.getQualifiedName().toString(), id);
        final TypeElement defaultImplementationOf = getDefaultImplementationOf(defaultImplementation);
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeOf = typeUtils.erasure(defaultImplementationOf.asType());
        boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeOf);
        if (!assignable)
        {
            processingEnvironment.getMessager()
                    .printMessage(Diagnostic.Kind.WARNING, asTypeImpl.toString() + " can't be assigned to " + asTypeOf, implementingClassTypeElement);
            return new BitSet();
        }
        if (InjectionContext.isInjectableEverywhere(context))
        {
            if (notGenerated(Scopes.Common, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Common, generated);
                generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                if (defaultImplementation.export())
                {
                    exportedInterface.set(Scopes.Server.ordinal());
                }
            }
        }
        else if (InjectionContext.isInjectableOnClient(context))
        {
            if (notGenerated(Scopes.Client, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Client, generated);
                generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                if (defaultImplementation.export())
                {
                    exportedInterface.set(Scopes.Server.ordinal());
                }
            }
        }
        else
        {
            if (InjectionContext.isInjectableOnServer(context))
            {
                if (notGenerated(Scopes.Server, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.Server, generated);
                    generateDefaultImplementation(implementingClassTypeElement, interfaceTypeElement, moduleWriter);
                    if (defaultImplementation.export())
                    {
                        exportedInterface.set(Scopes.Server.ordinal());
                    }
                }
            }
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
            else if (InjectionContext.isInjectableOnSwing(context))
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
        moduleWriter.println();
        moduleWriter.println("@Provides");

        String implementingName = implementingClassTypeElement.getQualifiedName().toString();
        moduleWriter.println(
                "public " + interfaceName + " " + createMethodName(moduleWriter, implementingClassTypeElement) + "(" + implementingName + " result) {");
        moduleWriter.indent();
        moduleWriter.println("return result;");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private void generateWebserviceComponent(String artifactName, TypeElement implementingClassTypeElement, TypeElement interfaceName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        moduleWriter.println("@Provides(type=Type.MAP) @" + Singleton.class.getCanonicalName());
        moduleWriter.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + implementingClassTypeElement.getQualifiedName().toString() + "\")");
        moduleWriter.println("public " + MembersInjector.class.getCanonicalName() + " " +
                createMethodName(moduleWriter, implementingClassTypeElement) + "(" + MembersInjector.class.getCanonicalName() + "<"
                + implementingClassTypeElement.getQualifiedName().toString() + "> injector){");
        moduleWriter.indent();
        moduleWriter.println("return injector;");
        moduleWriter.outdent();
        moduleWriter.println("}");
        moduleWriter.println();
        moduleWriter.println("@Provides(type=Type.MAP) @" + Singleton.class.getCanonicalName());
        moduleWriter.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + implementingClassTypeElement.getQualifiedName().toString() + "\")");
        moduleWriter.println("public " + Class.class.getCanonicalName() + " " + createMethodName(moduleWriter, implementingClassTypeElement) + "Class(){");
        moduleWriter.indent();
        moduleWriter.println("return " + implementingClassTypeElement.getQualifiedName().toString() + ".class;");
        moduleWriter.outdent();
        moduleWriter.println("}");
    }

    private String createMethodName(SourceWriter moduleWriter, final TypeElement typeElement)
    {
        final String methodName = typeElement.getSimpleName().toString();
        String finalMethodName = methodName;
        int counter = 0;
        while (moduleWriter.containsMethod(finalMethodName))
        {
            finalMethodName = methodName + counter;
            counter++;
        }
        moduleWriter.addMethod(finalMethodName);
        return finalMethodName;
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
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING, asTypeImpl.toString() + " is not assignable to " + asTypeInterface
                    + ". Please check the type and make a clean build to resolve the problem. ", implementingClassTypeElement);
            return;
        }
        final InjectionContext[] context = extensionPoint.context();
        final TypeElement defaultImpl = ((TypeElement) typeUtils.asElement(typeUtils.erasure(implementingClassTypeElement.asType())));
        final Generated generated = new Generated(interfaceElementType.getQualifiedName().toString(), defaultImpl.getQualifiedName().toString(),
                extension.id());
        if (InjectionContext.isInjectableEverywhere(context))
        {
            if (notGenerated(Scopes.Common, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Common, generated);
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        else if (InjectionContext.isInjectableOnClient(context))
        {
            if (notGenerated(Scopes.Client, generated))
            {
                SourceWriter moduleWriter = getWriter(Scopes.Client, generated);
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        else
        {
            if (InjectionContext.isInjectableOnServer(context))
            {
                if (notGenerated(Scopes.Server, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.Server, generated);
                    generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnGwt(context))
            {
                if (notGenerated(Scopes.Gwt, generated))
                {
                    SourceWriter moduleWriter = getWriter(Scopes.Gwt, generated);
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
    }

    private void generateExtensionMethods(Extension extension, final TypeElement interfaceName, final TypeElement defaultImplClassName,
            SourceWriter moduleWriter)
    {
        moduleWriter.println();
        writeMethod(interfaceName, moduleWriter, defaultImplClassName, true, extension);
        writeMethod(interfaceName, moduleWriter, defaultImplClassName, false, extension);
    }

    private void writeMethod(TypeElement interfaceName, SourceWriter moduleWriter, final TypeElement defaultImplClassName, final boolean isMap,
            final Extension extension)
    {
        final String methodSuffix;
        if (isMap)
        {
            moduleWriter.println("@Provides(type=Type.MAP)");// @javax.inject.Singleton");
            moduleWriter.println("@" + DaggerMapKey.class.getSimpleName() + "(\"" + extension.id() + "\")");
            //final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = "_Map";
        }
        else
        {
            moduleWriter.println();
            moduleWriter.println("@Provides(type=Type.SET)");//@javax.inject.Singleton");
            //final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = "_Set";
        }
        final String methodName = defaultImplClassName.getSimpleName() + "_" + methodSuffix;
        int counter = 0;
        String finalMethodName = methodName;
        while (moduleWriter.containsMethod(finalMethodName))
        {
            finalMethodName = methodName + counter;
            counter++;
        }
        moduleWriter.addMethod(finalMethodName);
        moduleWriter.println("public " + interfaceName + " " + finalMethodName + "(" + defaultImplClassName.getQualifiedName().toString() + " impl) {");
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
