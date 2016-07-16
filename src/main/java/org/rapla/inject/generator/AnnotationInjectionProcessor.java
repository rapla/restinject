package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;
import org.rapla.inject.generator.internal.SourceWriter;
import org.rapla.rest.generator.internal.GwtProxyCreator;
import org.rapla.rest.generator.internal.JavaClientProxyCreator;
import org.rapla.rest.generator.internal.ResultDeserializerCreator;
import org.rapla.rest.generator.internal.SerializerCreator;
import org.rapla.rest.generator.internal.TreeLogger;
import org.rapla.rest.generator.internal.UnableToCompleteException;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import dagger.multibindings.StringKey;

/*
 * Annotation Processor to create the org.rapla.servicelist file within the META-INF folder and one file 
 * for each extension point, default implementation of a class or interface and webservice within the META-INF/services folder.</br>
 * Scannes for the Annotations @Extension, @ExtensionRepeatable, @ExtensionPoint, @DefaultImplementation,
 * @DefaultImplementationRepeatable, @Path and @RemoteJsonMethod. </br>
 * E.g. for a @DefaultImplementation(of="org.example.Interface", context=InjectionContext.all) at a class called
 * "org.example.InterfaceImpl" a entry of org.example.Interface is added to the org.rapla.servicelist file and
 * an entry of org.example.InterfaceImpl is inserted in the org.example.Interface file within the service folder.
 */
public class AnnotationInjectionProcessor extends AbstractProcessor
{

    public final static JavaFileManager.Location META_INF_LOCATION = StandardLocation.CLASS_OUTPUT;
    private SerializerCreator serializerCreator;
    private ResultDeserializerCreator deserializerCreator;
    public final static String MODULE_NAME_OPTION = "moduleName";
    public final static String PARENT_MODULES_OPTION = "parentModules";

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions()
    {
        return Collections.singleton(MODULE_NAME_OPTION);
    }

    private final Class<?>[] supportedAnnotations = new Class[] { Extension.class, ExtensionRepeatable.class, ExtensionPoint.class, DefaultImplementation.class,
            DefaultImplementationRepeatable.class, Path.class, Provider.class };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        serializerCreator = new SerializerCreator(processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
        deserializerCreator = new ResultDeserializerCreator(serializerCreator, processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        Set<String> supported = new HashSet<String>();
        for (Class<?> annotationClass : supportedAnnotations)
        {
            final String canonicalName = annotationClass.getCanonicalName();
            supported.add(canonicalName);
        }
        return supported;
    }

    public static int counter = 0;

    @Override
    synchronized public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (roundEnv.processingOver() || annotations.isEmpty())
        {
            return false;
        }
        counter++;
        int i = counter;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Restinject Annotationprocessing starting for task " + i);
        try
        {
            for (TypeElement annotation : annotations)
            {
                final Set<? extends Element> element = roundEnv.getElementsAnnotatedWith(annotation);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Got " + element.toString());
            }
            ProcessedAnnotations processedAnnotations = preProcessAnnotationsAndCreateProxies(roundEnv);
            boolean daggerModuleRebuildNeeded = processedAnnotations.daggerModuleRebuildNeeded;
            Scopes scope = Scopes.Common;
            ModuleInfo moduleInfo = new ModuleInfo(processingEnv);
            final String fullModuleStarterName = moduleInfo.getFullModuleName( scope);
            if (processingEnv.getElementUtils().getTypeElement(fullModuleStarterName) == null && daggerModuleRebuildNeeded)
            {
                // Dagger
                processDagger(annotations, roundEnv, moduleInfo, processedAnnotations);
            }
        }
        catch (Exception ioe)
        {
            StringWriter stringWriter = new StringWriter();
            ioe.printStackTrace(new PrintWriter(stringWriter));
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
            return false;
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Restinject Annotationprocessing finished for task " + i);
        return true;
    }

    private ProcessedAnnotations preProcessAnnotationsAndCreateProxies(RoundEnvironment roundEnv) throws Exception
    {
        ProcessedAnnotations processedAnnotations = new ProcessedAnnotations();
        File interfaceListFile = getInterfaceList(processingEnv.getFiler());
        File interfaceListFileFolder = interfaceListFile.getParentFile();
        boolean daggerModuleRebuildNeeded = false;
        TreeLogger proxyLogger = new TreeLogger();
        boolean pathAnnotationFound = false;

        //        List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.gwt });
        for (final Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
        {
            final DefaultImplementation defaultImplementationAnnotation = elem.getAnnotation(DefaultImplementation.class);
            boolean pathAnnotationFoundInHandle = handleDefaultImplementationForType(interfaceListFile, interfaceListFileFolder, proxyLogger, elem,
                    defaultImplementationAnnotation, processedAnnotations);
            pathAnnotationFound |= pathAnnotationFoundInHandle;
            // we also created proxies for gwt and java, so rebuild is needed
            if (!isGeneratedByAnnotationInjectionProcessor(elem))
                daggerModuleRebuildNeeded = true;
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementationRepeatable.class))
        {
            final DefaultImplementationRepeatable annotation = elem.getAnnotation(DefaultImplementationRepeatable.class);
            for (final DefaultImplementation defaultImplementation : annotation.value())
            {
                boolean pathAnnotationFoundInHandle = handleDefaultImplementationForType(interfaceListFile, interfaceListFileFolder, proxyLogger, elem,
                        defaultImplementation, processedAnnotations);
                pathAnnotationFound |= pathAnnotationFoundInHandle;
                // we also created proxies for gwt and java, so rebuild is needed
                if (!isGeneratedByAnnotationInjectionProcessor(elem))
                    daggerModuleRebuildNeeded = true;
            }
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(ExtensionPoint.class))
        {
            final TypeElement typeElement = (TypeElement) elem;
            final String qualifiedName = typeElement.getQualifiedName().toString();
            appendToServiceList(interfaceListFile, qualifiedName);
            addServiceFile(typeElement, null, interfaceListFileFolder);
            daggerModuleRebuildNeeded = true;
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
        {
            final Extension annotation = elem.getAnnotation(Extension.class);
            handleExtension(interfaceListFile, interfaceListFileFolder, elem, annotation, processedAnnotations);
            daggerModuleRebuildNeeded = true;
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(ExtensionRepeatable.class))
        {
            final ExtensionRepeatable annotation = elem.getAnnotation(ExtensionRepeatable.class);
            final Extension[] value = annotation.value();
            for (final Extension extension : value)
            {
                handleExtension(interfaceListFile, interfaceListFileFolder, elem, extension, processedAnnotations);
            }
        }

        // Path
        for (Element elem : roundEnv.getElementsAnnotatedWith(Path.class))
        {
            if (elem instanceof TypeElement)
            {
                final TypeElement typeElement = (TypeElement) elem;
                // all interfaces annotated with path are handled with default implementation of server (@see handleDefaultImplementation)
                if (typeElement.getKind() != ElementKind.INTERFACE)
                {
                    addServiceFile(Path.class.getCanonicalName(), typeElement, interfaceListFileFolder);
                    pathAnnotationFound = true;
                    final String string = typeElement.getQualifiedName().toString();
                    processedAnnotations.addPath(string);
                }
            }
        }
        if (pathAnnotationFound)
        {
            appendToServiceList(interfaceListFile, Path.class.getCanonicalName());
            daggerModuleRebuildNeeded = true;
        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(Provider.class))
        {
            if (elem instanceof TypeElement)
            {
                addServiceFile(Provider.class.getCanonicalName(), (TypeElement) elem, interfaceListFileFolder);
            }
        }
        processedAnnotations.daggerModuleRebuildNeeded = daggerModuleRebuildNeeded;
        return processedAnnotations;
    }

    private void handleExtension(File interfaceListFile, File interfaceListFileFolder, final Element elem, final Extension annotation,
            ProcessedAnnotations processedAnnotations) throws IOException, UnableToCompleteException
    {
        final TypeElement typeElement = (TypeElement) elem;
        final TypeElement extensionPoint = getProvides(annotation);
        checkImplements(typeElement, extensionPoint, Extension.class.getCanonicalName());
        final String interfaceQualifiedName = extensionPoint.getQualifiedName().toString();
        appendToServiceList(interfaceListFile, interfaceQualifiedName);
        addServiceFile(extensionPoint, typeElement, interfaceListFileFolder);
        processedAnnotations.addImplementation(interfaceQualifiedName, typeElement.getQualifiedName().toString());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding extension " + typeElement);
    }

    private void checkImplements(TypeElement typeElement, TypeElement extensionPoint, String annotationName) throws UnableToCompleteException
    {
        if (!isImplementing(typeElement, extensionPoint))
        {
            boolean isInterface = typeElement.getKind() == ElementKind.INTERFACE;
            throw new UnableToCompleteException(
                    typeElement.toString() + " has a declared " + annotationName + " but does not " + (isInterface ? "extend" : "implement") + " "
                            + extensionPoint.toString() + ". You need to add " + (isInterface ? "extends" : "implements") + " " + extensionPoint.toString());
        }
    }

    private boolean isImplementing(TypeElement implementingElement, TypeElement interfaceToImplement)
    {
        final DeclaredType providedInterface = processingEnv.getTypeUtils().getDeclaredType(interfaceToImplement);
        String providedTypeString = SerializerCreator.erasedTypeString(providedInterface, processingEnv);
        final List<? extends TypeMirror> implementedInterfaces = implementingElement.getInterfaces();
        for (TypeMirror implementedInterface : implementedInterfaces)
        {
            String typeString = SerializerCreator.erasedTypeString(implementedInterface, processingEnv);
            if (providedTypeString.equals(typeString))
            {
                return true;
            }
            final Element implementedInterfaceElement = processingEnv.getTypeUtils().asElement(implementedInterface);
            if (implementedInterfaceElement instanceof TypeElement)
            {
                if (isImplementing((TypeElement) implementedInterfaceElement, interfaceToImplement))
                {
                    return true;
                }
            }
        }

        final TypeMirror superclass = implementingElement.getSuperclass();
        if (superclass != null)
        {
            String typeString = SerializerCreator.erasedTypeString(superclass, processingEnv);
            if (providedTypeString.equals(typeString))
            {
                return true;
            }
            final Element superClassElement = processingEnv.getTypeUtils().asElement(superclass);
            if (superClassElement instanceof TypeElement)
            {
                if (isImplementing((TypeElement) superClassElement, interfaceToImplement))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleDefaultImplementationForType(File interfaceListFile, File interfaceListFileFolder, TreeLogger proxyLogger,
            final Element defaultImplementationElement, DefaultImplementation defaultImplementationAnnotation, ProcessedAnnotations processedAnnotations)
            throws UnableToCompleteException, IOException
    {
        if (isGeneratedByAnnotationInjectionProcessor(defaultImplementationElement))
        {
            return false;
        }
        final TypeElement implementationElement = (TypeElement) defaultImplementationElement;
        final TypeElement interfaceElement = getDefaultImplementationOf(defaultImplementationAnnotation);
        checkImplements(implementationElement, interfaceElement, DefaultImplementation.class.getCanonicalName());
        boolean pathAnnotationFound = false;
        // Check if we need to generate proxies for java or gwt 
        if (interfaceElement.getAnnotation(Path.class) != null && interfaceElement.getKind() == ElementKind.INTERFACE)
        {
            final GwtProxyCreator proxyCreator = new GwtProxyCreator(interfaceElement, processingEnv, serializerCreator, deserializerCreator,
                    AnnotationInjectionProcessor.class.getCanonicalName());
            processedAnnotations.addPath(implementationElement.getQualifiedName().toString());
            final String proxyClassName = proxyCreator.create(proxyLogger);

            final JavaClientProxyCreator swingProxyCreator = new JavaClientProxyCreator(interfaceElement, processingEnv, serializerCreator, deserializerCreator,
                    AnnotationInjectionProcessor.class.getCanonicalName());
            final String swingproxyClassName = swingProxyCreator.create(proxyLogger);
            {
                final String qualifiedName = interfaceElement.getQualifiedName().toString();
                appendToServiceList(interfaceListFile, qualifiedName);
                final File serviceFile = getFile(interfaceListFile.getParentFile(), "services/" + qualifiedName);
                appendToFile(serviceFile, proxyClassName);
                appendToFile(serviceFile, swingproxyClassName);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Proxies " + proxyClassName + ", " + swingproxyClassName,
                        interfaceElement);
            }
            {
                final File serviceFile = getFile(interfaceListFile.getParentFile(), "services/" + Path.class.getCanonicalName());
                appendToFile(serviceFile, implementationElement.getQualifiedName().toString());
                pathAnnotationFound = true;
            }
        }
        final String qualifiedName = interfaceElement.getQualifiedName().toString();
        appendToServiceList(interfaceListFile, qualifiedName);
        addServiceFile(interfaceElement, implementationElement, interfaceListFileFolder);
        processedAnnotations.addImplementation(qualifiedName, implementationElement.getQualifiedName().toString());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding DefaultImplemenation " + implementationElement);
        return pathAnnotationFound;
    }

    private boolean isGeneratedByAnnotationInjectionProcessor(Element element)
    {
        final javax.annotation.Generated generatedAnnotation = element.getAnnotation(javax.annotation.Generated.class);
        final boolean generatedByAnnotationInjectionProcessor = generatedAnnotation != null
                && generatedAnnotation.value()[0].equals(AnnotationInjectionProcessor.class.getCanonicalName());
        return generatedByAnnotationInjectionProcessor;
    }

    private void appendToServiceList(File serviceListFile, String interfaceName) throws IOException
    {
        appendToFile(serviceListFile, interfaceName);
    }

    private void addServiceFile(TypeElement interfaceElement, TypeElement implementationElement, File folder) throws IOException, UnableToCompleteException
    {
        final String serviceFileName = getClassname(interfaceElement);
        addServiceFile(serviceFileName, implementationElement, folder);

    }

    private void addServiceFile(String serviceFileName, TypeElement implementationElement, File folder) throws IOException, UnableToCompleteException
    {
        String implementationName = implementationElement != null ? getClassname(implementationElement) : null;
        appendToFile("services/" + serviceFileName, folder, implementationName);
    }

    private String getClassname(TypeElement element) throws UnableToCompleteException
    {
        final NestingKind nestingKind = element.getNestingKind();
        if (nestingKind.equals(NestingKind.TOP_LEVEL))
        {
            return element.getQualifiedName().toString();
        }
        else if (nestingKind.equals(NestingKind.MEMBER))
        {
            final Element enclosingElement = element.getEnclosingElement();
            if (!(enclosingElement instanceof TypeElement))
            {
                throw new UnableToCompleteException("Only named Innerclasses are supported");
            }
            final String enclosingName = getClassname(((TypeElement) enclosingElement));
            return enclosingName + '$' + element.getSimpleName().toString();
        }
        else
        {
            throw new UnableToCompleteException("Only named Innerclasses are supported");
        }
    }

    public static void appendToFile(String serviceFileName, File folder, String line) throws IOException
    {
        final File serviceFile = getFile(folder, serviceFileName);
        if (line == null)
        {
            appendToFile(serviceFile, null);
        }
        else
        {
            appendToFile(serviceFile, line);
        }
    }

    static public File getFile(File folder, String filename)
    {
        final File serviceFile = new File(folder, filename);
        serviceFile.getParentFile().mkdirs();
        return serviceFile;
    }

    private static void appendToFile(File file, String className) throws IOException
    {
        if (className != null && file.exists())
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            try
            {
                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    if (line.equals(className))
                    {
                        reader.close();
                        return;
                    }
                }
            }
            finally
            {
                reader.close();
            }
        }
        PrintWriter w = new PrintWriter(new FileOutputStream(file, true));
        if (className != null)
        {
            w.write(className + "\n");
        }
        w.close();
    }

    public static List<String> readLines(File f) throws IOException
    {
        List<String> lines = new ArrayList<String>();
        try (final BufferedReader reader = new BufferedReader(new FileReader(f)))
        {
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
                lines.add(line);
            }
        }
        return lines;
    }

    public static File getInterfaceList(Filer filer) throws IOException
    {
        CharSequence pkg = "";
        JavaFileManager.Location location = META_INF_LOCATION;

        File f;
        try
        {
            FileObject resource = filer.getResource(location, pkg, InjectionContext.MODULE_LIST);
            f = new File(resource.toUri());
        }
        catch (IOException ex)
        {
            FileObject resource = filer.createResource(location, pkg, InjectionContext.MODULE_LIST);
            f = new File(resource.toUri());
        }
        f.getParentFile().mkdirs();
        return f;
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
        Types TypeUtils = this.processingEnv.getTypeUtils();
        return (TypeElement) TypeUtils.asElement(typeMirror);
    }

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

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        Common("common.dagger"), Server("server.dagger"), Webservice("server.dagger"), Client("client.dagger"), JavaClient("client.swing.dagger"), Gwt(
                "client.gwt.dagger");

        final String packageNameSuffix;

        static Scopes[] components()
        {
            return new Scopes[] { Server, JavaClient, Gwt };
        }

        Scopes(String packageNameSuffix)
        {
            this.packageNameSuffix = packageNameSuffix;
        }

        @Override
        public String toString()
        {
            return name();
        }

        public String getPackageName(String originalPackageName)
        {
            final String packageNameWithPoint = originalPackageName + (originalPackageName.length() == 0 ? "" : ".");
            final String packageName = packageNameWithPoint + packageNameSuffix;
            return packageName;
        }
    }

    synchronized public boolean processDagger(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ModuleInfo moduleInfo,
            ProcessedAnnotations processedAnnotations)
    {
        if (roundEnv.processingOver() || annotations.isEmpty())
        {
            return false;
        }
        try
        {
            process(moduleInfo, processedAnnotations);
        }
        catch (Exception ioe)
        {
            StringWriter stringWriter = new StringWriter();
            ioe.printStackTrace(new PrintWriter(stringWriter));
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
            return false;
        }
        return true;
    }

    private SourceWriter getWriter(Map<Scopes, SourceWriter> writerMap, Scopes scope, Generated generated)
    {
        Set<Generated> generatedSet = alreadyGenerated.get(scope);
        if (generatedSet == null)
        {
            generatedSet = new HashSet<>();
            alreadyGenerated.put(scope, generatedSet);
        }
        generatedSet.add(generated);
        return writerMap.get(scope);
    }

    private final Map<Scopes, Set<Generated>> alreadyGenerated = new LinkedHashMap<>();
    private final Map<Scopes, SourceWriter> moduleSourceWriters = new LinkedHashMap<>();
    private final Map<Scopes, SourceWriter> componentSourceWriters = new LinkedHashMap<>();
    private final Map<Scopes, Set<String>> exportedInterfaces = new LinkedHashMap<>();
    //private final Map<String, byte[]> existingWriters = new LinkedHashMap<>();
    private final String generatorName = AnnotationInjectionProcessor.class.getCanonicalName();

    synchronized private void process(ModuleInfo moduleInfo, ProcessedAnnotations processedAnnotations) throws Exception
    {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Starting Dagger creation for module " + moduleInfo);
        moduleSourceWriters.clear();
        componentSourceWriters.clear();
        // don't clear
        //existingWriters.clear();

        final long start = System.currentTimeMillis();
        Set<Scopes> scopes = new HashSet<>();
        scopes.addAll(Arrays.asList(Scopes.values()));
        scopes.remove(Scopes.Webservice);
        //        if (injectionContexts.contains(InjectionContext.all))
        //        {
        //            scopes.addAll(Arrays.asList(Scopes.values()));
        //        }
        //        if (injectionContexts.contains(InjectionContext.server))
        //        {
        //            scopes.add(Scopes.Server);
        //        }
        //        if (injectionContexts.contains(InjectionContext.client))
        //        {
        //            scopes.add(Scopes.Client);
        //            scopes.add(Scopes.Gwt);
        //            scopes.add(Scopes.JavaClient);
        //        }
        //        if (injectionContexts.contains(InjectionContext.gwt))
        //        {
        //            scopes.add(Scopes.Gwt);
        //        }
        //        if (injectionContexts.contains(InjectionContext.swing))
        //        {
        //            scopes.add(Scopes.JavaClient);
        //        }

        {
            generateModuleClass(moduleInfo, scopes, processedAnnotations);
            generateComponents(moduleInfo, scopes, processedAnnotations);
        }
        final long ms = System.currentTimeMillis() - start;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Finished Dagger creation for module  " + moduleInfo + " took " + ms + " ms");
    }

    public static class ModuleInfo
    {
        final public String groupId;
        final public String artifactId;
        final Set<ModuleInfo> parentModules = new HashSet<ModuleInfo>();

        public ModuleInfo(ProcessingEnvironment processingEnv) 
        {
            this(getModuleName(processingEnv));
            final String[] parents = getParentModuleNames(processingEnv);
            for (String parent:parents)
            {
                ModuleInfo parentModule = new ModuleInfo( parent );
                parentModules.add(parentModule);
            }
        }
        
        private ModuleInfo(String moduleName) 
        {
            final int i = moduleName.lastIndexOf(".");
            groupId = (i >= 0 ? moduleName.substring(0, i) : "");
            artifactId = firstCharUp(i >= 0 ? moduleName.substring(i + 1) : moduleName);
        }

        static public String getModuleName(ProcessingEnvironment processingEnv)
        {
            String moduleName = null;
            moduleName = processingEnv.getOptions().get(AnnotationInjectionProcessor.MODULE_NAME_OPTION);
            if (moduleName != null)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found param moduleName: " + moduleName);
                return moduleName;
            }
            moduleName = "org.rapla.rapla";
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "moduleName not found using " + moduleName);
            return moduleName;
        }
        
          
        
        
        private String[] getParentModuleNames(ProcessingEnvironment processingEnv)
        {
            String modules = processingEnv.getOptions().get(AnnotationInjectionProcessor.PARENT_MODULES_OPTION);
            if ( modules != null)
            {
                return modules.split(",");
            }
            return new String[] {};
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }
        
        public Set<ModuleInfo> getParentModules()
        {
            return parentModules;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            final String string = getModuleName();
            builder.append( string);
            if ( parentModules.size() >= 0)
            {
                builder.append("<--");
                boolean first = true;
                for (ModuleInfo parent:parentModules)
                {
                    if ( first)
                    {
                        first = false;
                    }
                    else
                    {
                        builder.append(", ");
                    }
                    builder.append(parent.getModuleName());
                }
            }
            return builder.toString();
        }

        private String getModuleName()
        {
            final String string = groupId + "." + artifactId;
            return string;
        }
        
        public String getScopedModuleName(Scopes scope)
        {
            String moduleName = scope.getPackageName(getGroupId())+ ".Dagger"+getArtifactId() + scope.toString() + "Module";
            return moduleName;
            
        }
        public String getScopedComponentName(Scopes scope)
        {
            String scopedPackageName = scope.getPackageName(getGroupId());
            final String componentName = scopedPackageName + "." + getArtifactId() + scope.toString() + "Component";
            return componentName;
        }

        public String getFullModuleName(Scopes scope)
        {
            return scope.getPackageName(groupId) + "." + getSimpleModuleName( scope);
        }
        
        public String getFullStartupModuleName(Scopes scope)
        {
            return scope.getPackageName(groupId) + "." + "Dagger" + artifactId + scope+ "StartupModule";
        }

        private String getSimpleModuleName( Scopes scope)
        {
            return "Dagger" + artifactId + scope + "Module";
        }
    }

    /**
     * Reads the interfaces defined in META-INF/org.rapla.servicelist into the provided set
     * and returns the File.
     * @param processedAnnotations 
     */

    private void generateModuleClass(ModuleInfo moduleInfo, Collection<Scopes> scopes, ProcessedAnnotations processedAnnotations) throws Exception
    {
        for (Scopes scope : scopes)
        {
            createSourceWriter(moduleInfo, scope);
        }

        File f = AnnotationInjectionProcessor.getInterfaceList(processingEnv.getFiler());
        List<String> interfaces = AnnotationInjectionProcessor.readLines(f);
        Set<String> allInterfaces = new LinkedHashSet<>(interfaces);
        allInterfaces.addAll(processedAnnotations.getExtensionPoints());
        for (String interfaceName : allInterfaces)
        {
            createMethods(interfaceName, processedAnnotations);
        }
        for (SourceWriter moduleWriter : moduleSourceWriters.values())
        {
            moduleWriter.outdent();
            moduleWriter.println("}");
            moduleWriter.close();
        }
    }

    public static class ProcessedAnnotations
    {
        private Set<String> paths = new LinkedHashSet<>();
        public boolean daggerModuleRebuildNeeded;
        private Map<String, Set<String>> implementingClasses = new LinkedHashMap<>();
        private Collection<String> extensionPoints = new LinkedHashSet<>();

        public Collection<String> getPaths()
        {
            return paths;
        }

        public Collection<String> getExtensionPoints()
        {
            return extensionPoints;
        }

        public Collection<String> getImplementations(String interfaceName)
        {
            final Set<String> set = implementingClasses.get(interfaceName);
            if (set == null)
            {
                return Collections.emptySet();
            }
            return set;
        }

        public void addImplementation(String interfaceName, String implementingClass)
        {
            Set<String> set = implementingClasses.get(interfaceName);
            if (set == null)
            {
                set = new LinkedHashSet<>();
                implementingClasses.put(interfaceName, set);
            }
            set.add(implementingClass);
        }

        public void addExtensionPoint(String extensionPoint)
        {
            extensionPoints.add(extensionPoint);
        }

        public void addPath(String path)
        {
            paths.add(path);
        }

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
    private void generateComponents(ModuleInfo moduleInfo, Collection<Scopes> scopes, ProcessedAnnotations processedAnnotations) throws Exception
    {
        //Map<String, BitSet> exportedInterfaces = new LinkedHashMap<>();
        for (Scopes scope : Scopes.components())
        {
            if (!scopes.contains(scope))
            {
                continue;
            }
            String fileName = "META-INF/" + getExportListFilename(scope);
            final Set<String> exportInterfaces = new LinkedHashSet<>();
            {
                exportInterfaces.addAll(loadLinesFromFile(fileName, processingEnv));
                final Set<String> set = this.exportedInterfaces.get(scope);
                if (set != null)
                {
                    exportInterfaces.addAll(set);
                }
            }
            SourceWriter writer = createComponentSourceWriter(moduleInfo, scope);
            for (String interfaceName : exportInterfaces)
            {
                String key = interfaceName;
                final int i = key.lastIndexOf(".");
                String simpleName = i > 0 ? key.substring(i + 1) : key;
                String javaname = firstCharUp(simpleName);
                writer.println(key + " get" + javaname + "();");
            }

            if (scope == Scopes.Server)
            {
                final String webserviceLocation = "META-INF/services/javax.ws.rs.Path";
                final Set<String> paths = new LinkedHashSet<>(loadLinesFromFile(webserviceLocation, processingEnv));
                paths.addAll(processedAnnotations.getPaths());
                for (String path : paths)
                {
                    String implementingClass = path;
                    final TypeElement implementingClassTypeElement = processingEnv.getElementUtils().getTypeElement(implementingClass);
                    final String methodName = toJavaName(implementingClassTypeElement);
                    writer.println("void " + "inject_" + methodName + "(" + implementingClassTypeElement.getQualifiedName().toString() + " injector);");
                }
            }
            writer.outdent();
            writer.println("}");
            writer.close();
            componentSourceWriters.put(scope, writer);
        }
    }

    private SourceWriter createComponentSourceWriter(ModuleInfo moduleInfo, Scopes scope) throws Exception
    {
        String originalPackageName = moduleInfo.getGroupId();
        String artifactId = moduleInfo.getArtifactId();

        String packageName = scope.getPackageName(originalPackageName);
        //        {
        //            final String file = "META-INF/" + getModuleListFileName(Scopes.Common);
        //            modules.addAll(loadLinesFromMetaInfo(file));
        //        }
        //        if ( scope == Scopes.Gwt || scope == Scopes.JavaClient)
        //        {
        //            final String file = "META-INF/" + getModuleListFileName(Scopes.Client);
        //            modules.addAll(loadLinesFromMetaInfo(file));
        //        }

        //        if (scope == Scopes.Server)
        //        {
        //            String moduleName = "Dagger" + firstCharUp(artifactId) + Scopes.WebserviceModule.toString() + "Module";
        //            String webserviceComponentModule = Scopes.WebserviceModule.getPackageName(originalPackageName) + "." + moduleName;
        //
        //            modules.add(webserviceComponentModule);
        //        }

        final String simpleComponentName = artifactId + scope.toString() + "Component";
        //final String componentName = packageName + "." + simpleComponentName;

        final SourceWriter writer = createSourceWriter(packageName, simpleComponentName);
        writer.println("package " + packageName + ";");
        writer.println();
        writer.println("import " + Inject.class.getCanonicalName() + ";");
        writer.println("import " + Singleton.class.getCanonicalName() + ";");
        writer.println("import " + Component.class.getCanonicalName() + ";");
        writer.println();
        writer.println(getGeneratorString());
        final String modules = getModules(moduleInfo, scope);
        writer.println("@Singleton @Component(" + modules + ")");
        writer.print("public interface " + simpleComponentName);
        final Set<ModuleInfo> parentModules = moduleInfo.getParentModules();
        boolean eclipseCompatibleMode = true;
        if (!eclipseCompatibleMode && !parentModules.isEmpty())
        {
            boolean first = true;
            writer.print(" extends ");
            for (ModuleInfo parent:parentModules)
            {
                if ( first )
                {
                    first = false;
                }
                else
                {
                    writer.print(", ");
                }
                
                final String parentComponentName = parent.getScopedComponentName( scope);
                writer.print( parentComponentName);
            }
        }
        writer.println(" {");
        writer.indent();
        if (eclipseCompatibleMode)
        {
            for(ModuleInfo module:parentModules)
            {
                final String parentComponentName = module.getScopedComponentName( scope);
                final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(parentComponentName);
                for (Element element: typeElement.getEnclosedElements())
                {
                    if (element.getKind() == ElementKind.METHOD)
                    {
                        ExecutableElement method = (ExecutableElement)element;
                        final TypeMirror returnType = method.getReturnType();
                        final List<? extends VariableElement> parameters = method.getParameters();
                        String methodName = method.getSimpleName().toString();
                        writer.print(returnType.toString());
                        writer.print(" " + methodName + "(");
                        for (VariableElement param:parameters)
                        {
                            String paramName = param.getSimpleName().toString();
                            String paramType =  param.asType().toString();
                            writer.print(paramType + " " + paramName);
                                
                        }
                        writer.print(");");
                        writer.println();    
                        
                        
                    }
                }
            }
        }
        return writer;
    }

    private String getModules(ModuleInfo moduleInfo, Scopes scope) throws IOException
    {
        StringBuilder buf = new StringBuilder();
        buf.append("modules = {");
        final Set<String> modules = new HashSet<>();
        final String file = "META-INF/" + getModuleListFileName(scope);
        final SourceWriter sourceWriter = moduleSourceWriters.get(scope);
        if (sourceWriter != null)
        {
            modules.add(sourceWriter.getQualifiedName());
        }
        modules.addAll(loadLinesFromFile(file, processingEnv));
        if (modules.size() == 0)
        {
            final String msg = "No Modules found for " + moduleInfo + scope.toString() + " File " + file;
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
        }
        Set<ModuleInfo> parentModules = moduleInfo.getParentModules();
        for (ModuleInfo parent:parentModules)
        {
            modules.add(parent.getScopedModuleName(scope));
        }
        boolean first = true;
        for (String module : modules)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                buf.append(",");
            }

            buf.append(module);
            buf.append(".class");
        }
        buf.append("}");
        return buf.toString();
    }

    private SourceWriter createSourceWriter(String packageName, String componentName)
    {
        return new SourceWriter(packageName, componentName, processingEnv);
    }

    static private Set<String> loadLinesFromFile(String file, ProcessingEnvironment processingEnv) throws IOException
    {
        if ( true)
        {
            return Collections.emptySet();
        }
        Set<String> foundLines = new LinkedHashSet<>();
        {
            final FileObject resource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", file);
            addFromFiler(resource, foundLines);
        }
        {
            final FileObject resource = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", file);
            addFromFiler(resource, foundLines);
        }
        {
            final ClassLoader classLoader = AnnotationInjectionProcessor.class.getClassLoader();
            addFromClassLoader(file, foundLines, classLoader);
        }
        {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            addFromClassLoader(file, foundLines, classLoader);
        }
        return foundLines;
    }

    private static void addFromClassLoader(String file, Set<String> foundLines, final ClassLoader classLoader) throws IOException, UnsupportedEncodingException
    {
        Enumeration<URL> resources = classLoader.getResources(file);
        //            if ((resources == null || !resources.hasMoreElements()) && !file.startsWith("/"))
        //            {
        //                resources = classLoader.getResources("/" + file);
        //            }
        if (resources != null)
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
    }

    private static void addFromFiler(final FileObject resource, Set<String> foundLines)
    {
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
            catch (IOException ex)
            {
            }
        }
    }

    void appendLineToMetaInf(String filename, String line) throws IOException
    {
        final File folder = getMetaInfFolder();
        AnnotationInjectionProcessor.appendToFile(filename, folder, line);
    }

    static public Collection<URL> getScanningUrlsFromClasspath(ClassLoader cl)
    {
        Collection<URL> result = new ArrayList<>();
        while (cl != null)
        {
            if (cl instanceof URLClassLoader)
            {
                URL[] urls = ((URLClassLoader) cl).getURLs();
                result.addAll(Arrays.asList(urls));
            }
            cl = cl.getParent();
        }
        return result;
    }

    private void createSourceWriter(ModuleInfo moduleInfo, Scopes scope) throws IOException
    {
        String originalPackageName = moduleInfo.getGroupId();
        String artifactId = moduleInfo.getArtifactId();
        String packageName = scope.getPackageName(originalPackageName);
        final String moduleListFile = getModuleListFileName(scope);
        {
            final String fullModuleName = moduleInfo.getFullModuleName( scope) ;
            appendLineToMetaInf(moduleListFile, fullModuleName);
        }
        final String simpleModuleName = moduleInfo.getSimpleModuleName( scope) ;
        final SourceWriter writer = createSourceWriter(packageName, simpleModuleName);
        writer.println("package " + packageName + ";");
        writer.println();
        writer.println("import " + Provides.class.getCanonicalName() + ";");
        writer.println("import " + Module.class.getCanonicalName() + ";");
        writer.println("import " + IntoMap.class.getCanonicalName() + ";");
        writer.println("import " + IntoSet.class.getCanonicalName() + ";");
        writer.println("import " + ElementsIntoSet.class.getCanonicalName() + ";");
        writer.println("import " + StringKey.class.getCanonicalName() + ";");
        writer.println();
        writer.println(getGeneratorString());
        writer.print("@Module(includes={");
        final String commonModuleName = moduleInfo.getFullModuleName( Scopes.Common) + ".class";
        final String clientModuleName = moduleInfo.getFullModuleName( Scopes.Client) + ".class";
        if (scope == Scopes.Server || scope == Scopes.Client)
        {
            writer.print(commonModuleName);
        }
        else if (scope != Scopes.Common)
        {
            writer.print(clientModuleName);
        }
        if (scope == Scopes.Server || scope == Scopes.Gwt || scope == Scopes.JavaClient)
        {
            // look for a Files ending with StarterModule and beginning with the daggermodule name
            // This files allow custom DaggerModuleCustomization

            final String fullModuleStarterName = moduleInfo.getFullStartupModuleName(scope);
            if (processingEnv.getElementUtils().getTypeElement(fullModuleStarterName) != null)
            {
                writer.print(",");
                writer.print(fullModuleStarterName + ".class");
            }
        }
        writer.println("})");
        writer.println("public class Dagger" + artifactId + scope + "Module {");
        writer.indent();
        moduleSourceWriters.put(scope, writer);
    }

  

    private String getModuleListFileName(Scopes scope)
    {
        String suffix = (scope == Scopes.Webservice) ? "Components" : "Modules";
        return "org.rapla.Dagger" + scope + suffix;
    }

    private String getExportListFilename(Scopes scope)
    {
        return "org.rapla.Dagger" + scope + "Exported";
    }

    private File getMetaInfFolder() throws IOException
    {
        return AnnotationInjectionProcessor.getInterfaceList(processingEnv.getFiler()).getParentFile();
    }

    void createMethods(String interfaceName, ProcessedAnnotations processedAnnotations) throws Exception
    {
        BitSet exportedInterface = new BitSet();
        File folder = getMetaInfFolder();
        final List<String> implementingClasses = AnnotationInjectionProcessor
                .readLines(AnnotationInjectionProcessor.getFile(folder, "services/" + interfaceName));
        interfaceName = interfaceName.replaceAll("\\$", ".");
        final TypeElement interfaceClassTypeElement = processingEnv.getElementUtils().getTypeElement(interfaceName);

        LinkedHashSet<String> allImplementingClasses = new LinkedHashSet<>(implementingClasses);
        final Collection<String> processedImplementingClasses = processedAnnotations.getImplementations(interfaceName);
        if (processedImplementingClasses != null)
        {
            allImplementingClasses.addAll(processedImplementingClasses);
        }
        for (String implementingClass : allImplementingClasses)
        {
            implementingClass = implementingClass.replaceAll("\\$", ".");
            final TypeElement implementingClassTypeElement = processingEnv.getElementUtils().getTypeElement(implementingClass);
            final Path path = implementingClassTypeElement != null ? implementingClassTypeElement.getAnnotation(Path.class) : null;
            if (interfaceClassTypeElement == null && path == null)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "No interface class found for " + interfaceName, implementingClassTypeElement);
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

                    Collection<DefaultImplementation> implementations = getDefaultImplementations(implementingClassTypeElement);
                    for (DefaultImplementation defaultImplementation : implementations)
                    {
                        final BitSet exported = generateDefaultImplementation(implementingClassTypeElement, interfaceClassTypeElement, defaultImplementation);
                        exportedInterface.or(exported);
                    }
                    Collection<Extension> extensions = getExtensions(implementingClassTypeElement);
                    for (Extension extension : extensions)
                    {
                        generateExtension(implementingClassTypeElement, interfaceClassTypeElement, extension);
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
                Set<String> set = exportedInterfaces.get(scope);
                if (set == null)
                {
                    set = new LinkedHashSet<>();
                    exportedInterfaces.put(scope, set);
                }
                set.add(interfaceName);
                appendLineToMetaInf(filename, interfaceName);
            }
        }
    }

    private Collection<DefaultImplementation> getDefaultImplementations(TypeElement typeElement)
    {
        Collection<DefaultImplementation> result = new ArrayList<>();
        {
            final DefaultImplementationRepeatable repeatable = typeElement.getAnnotation(DefaultImplementationRepeatable.class);
            if (repeatable != null)
            {
                result.addAll(Arrays.asList(repeatable.value()));
            }

            final DefaultImplementation single = typeElement.getAnnotation(DefaultImplementation.class);
            if (single != null)
            {
                result.add(single);
            }
        }
        return result;
    }

    private Collection<Extension> getExtensions(TypeElement typeElement)
    {
        Collection<Extension> result = new ArrayList<>();
        {
            final ExtensionRepeatable repeatable = typeElement.getAnnotation(ExtensionRepeatable.class);
            if (repeatable != null)
            {
                result.addAll(Arrays.asList(repeatable.value()));
            }

            final Extension single = typeElement.getAnnotation(Extension.class);
            if (single != null)
            {
                result.add(single);
            }
        }
        return result;
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
            final SourceWriter writer = getWriter(moduleSourceWriters, sourceWriterIndex, generated);
            writer.println();
            writer.println("@Provides");
            final String methodName = createMethodName(writer, interfaceClassTypeElement);
            writer.println("public " + interfaceName + " " + methodName + "(" + implementingClass + " result) {");
            writer.indent();
            writer.println("return result;");
            writer.outdent();
            writer.println("}");
        }
    }

    private String toJavaName(TypeElement className)
    {
        final Name qualifiedName = className.getQualifiedName();
        final String s = qualifiedName.toString().replaceAll("\\.", "_");
        return s;
    }

    static private String firstCharUp(String s)
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
                SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Common, generated);
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        else if (InjectionContext.isInjectableOnClient(context))
        {
            if (notGenerated(Scopes.Client, generated))
            {
                SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Client, generated);
                generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
            }
        }
        else
        {
            if (InjectionContext.isInjectableOnServer(context))
            {
                if (notGenerated(Scopes.Server, generated))
                {
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Server, generated);
                    generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnGwt(context))
            {
                if (notGenerated(Scopes.Gwt, generated))
                {
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Gwt, generated);
                    generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnSwing(context))
            {
                if (notGenerated(Scopes.JavaClient, generated))
                {
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.JavaClient, generated);
                    generateEmptyExtensionMethods(interfaceTypeElement, moduleWriter);
                }
            }
        }

    }

    private void generateEmptyExtensionMethods(final TypeElement interfaceName, SourceWriter writer)
    {
        writer.println();
        final String collectionTypeString;
        writer.println();
        writer.println("@javax.inject.Singleton @Provides @ElementsIntoSet");
        collectionTypeString = "Set";
        final String methodName = createMethodName(writer, interfaceName) + "_empty";
        writer.println("public java.util.Set<" + interfaceName.getQualifiedName().toString() + "> " + methodName + "_" + collectionTypeString + "() {");
        writer.indent();
        writer.println("return java.util.Collections.emptySet();");
        writer.outdent();
        writer.println("}");
    }

    private BitSet generateDefaultImplementation(final TypeElement implementingClassTypeElement, final TypeElement interfaceTypeElement,
            final DefaultImplementation defaultImplementation)
    {
        final String id = "DefaultImplementation";
        if (!getDefaultImplementationOf(defaultImplementation).equals(interfaceTypeElement))
        {
            return new BitSet();
        }
        BitSet exportedInterface = new BitSet();

        //final Set<InjectionContext> context = new HashSet<>(Arrays.asList(defaultImplementation.context());
        final InjectionContext[] context = defaultImplementation.context();
        final Types typeUtils = processingEnv.getTypeUtils();
        final Generated generated = new Generated(interfaceTypeElement.getQualifiedName().toString(),
                implementingClassTypeElement.getQualifiedName().toString(), id);
        final TypeElement defaultImplementationOf = getDefaultImplementationOf(defaultImplementation);
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeOf = typeUtils.erasure(defaultImplementationOf.asType());
        boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeOf);
        if (!assignable)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, asTypeImpl.toString() + " can't be assigned to " + asTypeOf,
                    implementingClassTypeElement);
            return new BitSet();
        }
        if (InjectionContext.isInjectableEverywhere(context))
        {
            if (notGenerated(Scopes.Common, generated))
            {
                SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Common, generated);
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
                SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Client, generated);
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
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Server, generated);
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
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Gwt, generated);
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
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.JavaClient, generated);
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
        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeMirror asTypeImpl = typeUtils.erasure(implementingClassTypeElement.asType());
        final TypeMirror asTypeInterface = typeUtils.erasure(interfaceProvided.asType());
        final boolean assignable = typeUtils.isAssignable(asTypeImpl, asTypeInterface);
        if (!assignable)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, asTypeImpl.toString() + " is not assignable to " + asTypeInterface
                    + ". Please check the type and make a clean build to resolve the problem. ", implementingClassTypeElement);
            return;
        }
        final InjectionContext[] context = extensionPoint.context();
        final TypeElement defaultImpl = ((TypeElement) typeUtils.asElement(typeUtils.erasure(implementingClassTypeElement.asType())));
        final String className = defaultImpl.getQualifiedName().toString();
        final String id = className;
        final Generated generated = new Generated(interfaceElementType.getQualifiedName().toString(), className, id);
        if (InjectionContext.isInjectableEverywhere(context))
        {
            if (notGenerated(Scopes.Common, generated))
            {
                SourceWriter writer = getWriter(moduleSourceWriters, Scopes.Common, generated);
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, writer);
            }
        }
        else if (InjectionContext.isInjectableOnClient(context))
        {
            if (notGenerated(Scopes.Client, generated))
            {
                SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Client, generated);
                generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
            }
        }
        else
        {
            if (InjectionContext.isInjectableOnServer(context))
            {
                if (notGenerated(Scopes.Server, generated))
                {
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Server, generated);
                    generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
                }
            }
            if (InjectionContext.isInjectableOnGwt(context))
            {
                if (notGenerated(Scopes.Gwt, generated))
                {
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.Gwt, generated);
                    generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
                }
            }

            if (InjectionContext.isInjectableOnSwing(context))
            {
                if (notGenerated(Scopes.JavaClient, generated))
                {
                    SourceWriter moduleWriter = getWriter(moduleSourceWriters, Scopes.JavaClient, generated);
                    generateExtensionMethods(extension, interfaceElementType, defaultImpl, moduleWriter);
                }
            }
        }
    }

    private void generateExtensionMethods(Extension extension, final TypeElement interfaceName, final TypeElement defaultImplClassName, SourceWriter writer)
    {
        writer.println();
        writeMethod(interfaceName, writer, defaultImplClassName, true, extension);
        writeMethod(interfaceName, writer, defaultImplClassName, false, extension);
    }

    private void writeMethod(TypeElement interfaceName, SourceWriter writer, final TypeElement defaultImplClassName, final boolean isMap,
            final Extension extension)
    {
        final String methodSuffix;
        if (isMap)
        {
            writer.println("@Provides");// @javax.inject.Singleton");
            writer.println("@IntoMap");
            writer.println("@StringKey(\"" + extension.id() + "\")");
            //final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = "_Map";
        }
        else
        {
            writer.println();
            writer.println("@Provides");//@javax.inject.Singleton");
            writer.println("@IntoSet");
            //final String id = extension.id().replaceAll("\\.", "_");
            methodSuffix = "_Set";
        }
        final String methodName = defaultImplClassName.getSimpleName() + "_" + methodSuffix;
        int counter = 0;
        String finalMethodName = methodName;
        while (writer.containsMethod(finalMethodName))
        {
            finalMethodName = methodName + counter;
            counter++;
        }
        writer.addMethod(finalMethodName);
        writer.println("public " + interfaceName + " " + finalMethodName + "(" + defaultImplClassName.getQualifiedName().toString() + " impl) {");
        writer.indent();
        writer.println("return impl;");
        writer.outdent();
        writer.println("}");
    }

}
