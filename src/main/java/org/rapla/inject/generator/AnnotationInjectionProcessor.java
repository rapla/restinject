package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
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
import org.rapla.rest.generator.internal.GwtProxyCreator;
import org.rapla.rest.generator.internal.JavaClientProxyCreator;
import org.rapla.rest.generator.internal.ResultDeserializerCreator;
import org.rapla.rest.generator.internal.SerializerCreator;
import org.rapla.rest.generator.internal.TreeLogger;
import org.rapla.rest.generator.internal.UnableToCompleteException;

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
    final DaggerModuleCreator daggerModuleProcessor = new DaggerModuleCreator();
    private SerializerCreator serializerCreator;
    private ResultDeserializerCreator deserializerCreator;
    public final static String MODULE_NAME_OPTION = "moduleName";

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    @Override public Set<String> getSupportedOptions()
    {
        return Collections.singleton(MODULE_NAME_OPTION);
    }

    private final Class<?>[] supportedAnnotations = new Class[] { Extension.class, ExtensionRepeatable.class, ExtensionPoint.class, DefaultImplementation.class,
            DefaultImplementationRepeatable.class, Path.class, Provider.class };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        daggerModuleProcessor.init( processingEnv);
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
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Got "+element.toString());
            }
            boolean daggerModuleRebuildNeeded = process(roundEnv);
            // only generate the modules if we processed a class
            if (daggerModuleRebuildNeeded)
            {
                // Dagger
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Dagger Modules");
                daggerModuleProcessor.process(annotations,roundEnv);
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

    private boolean process(RoundEnvironment roundEnv) throws Exception
    {
        File interfaceListFile = getInterfaceList(processingEnv.getFiler());
        File interfaceListFileFolder = interfaceListFile.getParentFile();
        boolean daggerModuleRebuildNeeded = false;
        TreeLogger proxyLogger = new TreeLogger();
        boolean pathAnnotationFound = false;

        //        List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.gwt });
        for (final Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
        {
            final DefaultImplementation defaultImplementationAnnotation = elem.getAnnotation(DefaultImplementation.class);
            boolean pathAnnotationFoundInHandle = handleDefaultImplemenatationForType(interfaceListFile, interfaceListFileFolder, proxyLogger, elem,
                    defaultImplementationAnnotation);
            pathAnnotationFound |= pathAnnotationFoundInHandle;
            // we also created proxies for gwt and java, so rebuild is needed
            if(!isGeneratedByAnnotationInjectionProcessor(elem))
                daggerModuleRebuildNeeded = true;
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementationRepeatable.class))
        {
            final DefaultImplementationRepeatable annotation = elem.getAnnotation(DefaultImplementationRepeatable.class);
            for (final DefaultImplementation defaultImplementation : annotation.value())
            {
                boolean pathAnnotationFoundInHandle = handleDefaultImplemenatationForType(interfaceListFile, interfaceListFileFolder, proxyLogger, elem,
                        defaultImplementation);
                pathAnnotationFound |= pathAnnotationFoundInHandle;
                // we also created proxies for gwt and java, so rebuild is needed
                if(!isGeneratedByAnnotationInjectionProcessor(elem))
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
            handleExtension(interfaceListFile, interfaceListFileFolder, elem, annotation);
            daggerModuleRebuildNeeded = true;
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(ExtensionRepeatable.class))
        {
            final ExtensionRepeatable annotation = elem.getAnnotation(ExtensionRepeatable.class);
            final Extension[] value = annotation.value();
            for (final Extension extension : value)
            {
                handleExtension(interfaceListFile, interfaceListFileFolder, elem, extension);
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
        return daggerModuleRebuildNeeded;
    }

    private void handleExtension(File interfaceListFile, File interfaceListFileFolder, final Element elem, final Extension annotation)
            throws IOException, UnableToCompleteException
    {
        final TypeElement typeElement = (TypeElement) elem;
        final TypeElement extensionPoint = getProvides(annotation);
        checkImplements(typeElement, extensionPoint, Extension.class.getCanonicalName());
        final String interfaceQualifiedName = extensionPoint.getQualifiedName().toString();
        appendToServiceList(interfaceListFile, interfaceQualifiedName);
        addServiceFile(extensionPoint, typeElement, interfaceListFileFolder);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding extension " + typeElement);
    }

    private void checkImplements(TypeElement typeElement, TypeElement extensionPoint, String annotationName) throws UnableToCompleteException
    {
        // TODO also check super classes
        if (!isImplementing(typeElement, extensionPoint))
        {
            throw new UnableToCompleteException(typeElement.toString() + " has a declared " + annotationName + " but does not directly implement " + extensionPoint.toString() +". You need to add implements " + extensionPoint.toString() + ". Super implementations are not checked.");
        }
    }

    private boolean isImplementing(TypeElement implementingElement, TypeElement interfaceToImplement)
    {
        final DeclaredType providedInterface = processingEnv.getTypeUtils().getDeclaredType(interfaceToImplement);
        String providedTypeString = SerializerCreator.erasedTypeString( providedInterface, processingEnv);
        final List<? extends TypeMirror> implementedInterfaces = implementingElement.getInterfaces();
        for ( TypeMirror implementedInterface:implementedInterfaces)
        {
            String typeString = SerializerCreator.erasedTypeString( implementedInterface, processingEnv);
            if (providedTypeString.equals( typeString))
            {
                return true;
            }
            final Element implementedInterfaceElement = processingEnv.getTypeUtils().asElement(implementedInterface);
            if ( implementedInterfaceElement instanceof  TypeElement)
            {
                if ( isImplementing( (TypeElement) implementedInterfaceElement, interfaceToImplement))
                {
                    return true;
                }
            }
        }

        final TypeMirror superclass = implementingElement.getSuperclass();
        if ( superclass != null)
        {
            String typeString = SerializerCreator.erasedTypeString( superclass, processingEnv);
            if (providedTypeString.equals( typeString))
            {
                return true;
            }
            final Element superClassElement = processingEnv.getTypeUtils().asElement(superclass);
            if ( superClassElement instanceof  TypeElement)
            {
                if (isImplementing((TypeElement)superClassElement,interfaceToImplement))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleDefaultImplemenatationForType(File interfaceListFile, File interfaceListFileFolder, TreeLogger proxyLogger,
            final Element defaultImplementationElement, DefaultImplementation defaultImplementationAnnotation) throws UnableToCompleteException, IOException
    {
        if(isGeneratedByAnnotationInjectionProcessor(defaultImplementationElement))
        {
            return false;
        }
        final TypeElement implementationElement = (TypeElement) defaultImplementationElement;
        final TypeElement interfaceElement = getDefaultImplementationOf(defaultImplementationAnnotation);
        checkImplements( implementationElement, interfaceElement, DefaultImplementation.class.getCanonicalName());
        boolean pathAnnotationFound = false;
        // Check if we need to generate proxies for java or gwt 
        if (interfaceElement.getAnnotation(Path.class) != null && interfaceElement.getKind() == ElementKind.INTERFACE)
        {
            final GwtProxyCreator proxyCreator = new GwtProxyCreator(interfaceElement, processingEnv, serializerCreator, deserializerCreator, AnnotationInjectionProcessor.class.getCanonicalName());
            final String proxyClassName = proxyCreator.create(proxyLogger);

            final JavaClientProxyCreator swingProxyCreator = new JavaClientProxyCreator(interfaceElement, processingEnv,serializerCreator, deserializerCreator, AnnotationInjectionProcessor.class.getCanonicalName()
                    );
            final String swingproxyClassName = swingProxyCreator.create(proxyLogger);
            {
                final String qualifiedName = interfaceElement.getQualifiedName().toString();
                appendToServiceList(interfaceListFile, qualifiedName);
                final File serviceFile = getFile(interfaceListFile.getParentFile(), "services/" + qualifiedName);
                appendToFile(serviceFile, proxyClassName);
                appendToFile(serviceFile, swingproxyClassName);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Proxies " + proxyClassName + ", " + swingproxyClassName);
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding DefaultImplemenation " + implementationElement);
        return pathAnnotationFound;
    }

    private boolean isGeneratedByAnnotationInjectionProcessor(Element element)
    {
        final Generated generatedAnnotation = element.getAnnotation(Generated.class);
        final boolean generatedByAnnotationInjectionProcessor = generatedAnnotation != null && generatedAnnotation.value()[0].equals(AnnotationInjectionProcessor.class.getCanonicalName());
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
}
