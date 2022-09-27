package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;
import org.rapla.rest.generator.internal.JavaClientProxyCreator;
import org.rapla.rest.generator.internal.SerializeCheck;
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

    public static final JavaFileManager.Location META_INF_LOCATION_FOR_ECLIPSE = StandardLocation.SOURCE_OUTPUT;
    public final static JavaFileManager.Location META_INF_LOCATION = StandardLocation.CLASS_OUTPUT;

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

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    private final Class<?>[] supportedAnnotations = new Class[] { Extension.class, ExtensionRepeatable.class, ExtensionPoint.class, DefaultImplementation.class,
            DefaultImplementationRepeatable.class, Path.class, Provider.class };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
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
            preProcessAnnotationsAndCreateProxies(roundEnv);
        }
        catch (Exception ioe)
        {
            StringWriter stringWriter = new StringWriter();
            ioe.printStackTrace(new PrintWriter(stringWriter));
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
            return true;
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Restinject Annotationprocessing finished for task " + i);
        return true;
    }

    private void preProcessAnnotationsAndCreateProxies(RoundEnvironment roundEnv) throws Exception
    {
        boolean pathAnnotationFound = false;

        //        List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.gwt });
        for (final Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
        {
            final DefaultImplementation defaultImplementationAnnotation = elem.getAnnotation(DefaultImplementation.class);
            boolean pathAnnotationFoundInHandle = handleDefaultImplementationForType( elem,
                    defaultImplementationAnnotation);
            pathAnnotationFound |= pathAnnotationFoundInHandle;
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementationRepeatable.class))
        {
            final DefaultImplementationRepeatable annotation = elem.getAnnotation(DefaultImplementationRepeatable.class);
            for (final DefaultImplementation defaultImplementation : annotation.value())
            {
                boolean pathAnnotationFoundInHandle = handleDefaultImplementationForType(elem,
                        defaultImplementation);
                pathAnnotationFound |= pathAnnotationFoundInHandle;
            }
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(ExtensionPoint.class))
        {
            final TypeElement typeElement = (TypeElement) elem;
            final String qualifiedName = typeElement.getQualifiedName().toString();
            appendToServiceList(qualifiedName);
            addServiceFile(typeElement, null);
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
        {
            final Extension annotation = elem.getAnnotation(Extension.class);
            handleExtension(elem, annotation);
        }

        for (final Element elem : roundEnv.getElementsAnnotatedWith(ExtensionRepeatable.class))
        {
            final ExtensionRepeatable annotation = elem.getAnnotation(ExtensionRepeatable.class);
            final Extension[] value = annotation.value();
            for (final Extension extension : value)
            {
                handleExtension(elem, extension);
            }
        }

        // Path
        // Create Proxies
        for (Element elem : roundEnv.getElementsAnnotatedWith(Path.class))
        {
            if (elem instanceof TypeElement)
            {
                final TypeElement typeElement = (TypeElement) elem;
                // all interfaces annotated with path are handled with default implementation of server (@see handleDefaultImplementation)
                if (typeElement.getKind() != ElementKind.INTERFACE)
                {
                    addServiceFile(Path.class.getCanonicalName(), typeElement);
                    pathAnnotationFound = true;
                    final String string = typeElement.getQualifiedName().toString();
                }
            }
        }
        if (pathAnnotationFound)
        {
            appendToServiceList(Path.class.getCanonicalName());
        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(Provider.class))
        {
            if (elem instanceof TypeElement)
            {
                addServiceFile(Provider.class.getCanonicalName(), (TypeElement) elem);
            }
        }
    }

    private void handleExtension(final Element elem, final Extension annotation) throws IOException, UnableToCompleteException
    {
        final TypeElement typeElement = (TypeElement) elem;
        final TypeElement extensionPoint = getProvides(annotation);
        checkImplements(typeElement, extensionPoint, Extension.class.getCanonicalName());
        final String interfaceQualifiedName = extensionPoint.getQualifiedName().toString();
        appendToServiceList(interfaceQualifiedName);
        addServiceFile(extensionPoint, typeElement);
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
        String providedTypeString = SerializeCheck.erasedTypeString(providedInterface, processingEnv);
        final List<? extends TypeMirror> implementedInterfaces = implementingElement.getInterfaces();
        for (TypeMirror implementedInterface : implementedInterfaces)
        {
            String typeString = SerializeCheck.erasedTypeString(implementedInterface, processingEnv);
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
            String typeString = SerializeCheck.erasedTypeString(superclass, processingEnv);
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

    private boolean isGeneratedByAnnotationInjectionProcessor(Element element)
    {
        final javax.annotation.Generated generatedAnnotation = element.getAnnotation(javax.annotation.Generated.class);
        final boolean generatedByAnnotationInjectionProcessor = generatedAnnotation != null
                && generatedAnnotation.value()[0].equals(AnnotationInjectionProcessor.class.getCanonicalName());
        return generatedByAnnotationInjectionProcessor;
    }

    private void appendToServiceList(String interfaceName) throws IOException
    {
        appendToFile(InjectionContext.MODULE_FILE_NAME, interfaceName);
    }

    
    private static JavaFileManager.Location[] LOCATIONS = new JavaFileManager.Location[]{META_INF_LOCATION, META_INF_LOCATION_FOR_ECLIPSE};
    private void appendToFile(String serviceFileName, String line) throws IOException
    {
        for (Location location : LOCATIONS)
        {// TODO Doppelt ausführen
            final File folder = getModulesFileInLocation(location).getParentFile();
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
    }

    private boolean handleDefaultImplementationForType(
            final Element defaultImplementationElement, DefaultImplementation defaultImplementationAnnotation)
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
        // Check if we need to generate proxies for java
        if (interfaceElement.getAnnotation(Path.class) != null && interfaceElement.getKind() == ElementKind.INTERFACE)
        {
            final JavaClientProxyCreator swingProxyCreator = new JavaClientProxyCreator(interfaceElement, processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
            final String swingproxyClassName = swingProxyCreator.create();
            {
                final String qualifiedName = getClassname(interfaceElement,false);
                appendToServiceList(qualifiedName);
                appendToFile("services/" + qualifiedName, swingproxyClassName);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Proxies " + swingproxyClassName,
                        interfaceElement);
            }
            {
                appendToFile("services/" + Path.class.getCanonicalName(), implementationElement.getQualifiedName().toString());
                pathAnnotationFound = true;
            }
        }
        final String qualifiedName = getClassname(interfaceElement);
        appendToServiceList(qualifiedName);
        addServiceFile(interfaceElement, implementationElement);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding DefaultImplemenation " + implementationElement);
        return pathAnnotationFound;
    }


    private void addServiceFile(TypeElement interfaceElement, TypeElement implementationElement) throws IOException, UnableToCompleteException
    {
        final String serviceFileName = getClassname(interfaceElement, false);
        addServiceFile(serviceFileName, implementationElement);

    }

    private void addServiceFile(String serviceFileName, TypeElement implementationElement) throws IOException, UnableToCompleteException
    {
        String implementationName = implementationElement != null ? getClassname(implementationElement) : null;
        appendToFile("services/" + serviceFileName,  implementationName);
    }

    private String getClassname(TypeElement element) throws UnableToCompleteException
    {
        return getClassname( element, true);
    }

    private String getClassname(TypeElement element, boolean addGenerics) throws UnableToCompleteException
    {
        final NestingKind nestingKind = element.getNestingKind();
        // FIXME enable generics later
        addGenerics = false;
        if (nestingKind.equals(NestingKind.TOP_LEVEL))
        {
            final List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
            final String s = element.getQualifiedName().toString();
            if ( typeParameters.isEmpty() || !addGenerics)
            {
                return s;
            }
            else
            {
                List<String> types = typeParameters.stream().map(( param) -> param.asType().toString()).collect(Collectors.toList());
                return s + "<" + String.join(",", types)+">";
            }

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

    private File getFile(File folder, String filename)
    {
        final File serviceFile = new File(folder, filename);
        serviceFile.getParentFile().mkdirs();
        return serviceFile;
    }

    private void appendToFile(File file, String className) throws IOException
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

    public List<String> readLines(File f) throws IOException
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

    private File getModulesFileInLocation(JavaFileManager.Location location) throws IOException
    {
        CharSequence pkg = "";

        File f;
        try
        {
            FileObject resource = processingEnv.getFiler().getResource(location, pkg, InjectionContext.MODULE_LIST_LOCATION);
            f = new File(resource.toUri());
        }
        catch (IOException ex)
        {
            FileObject resource = processingEnv.getFiler().createResource(location, pkg, InjectionContext.MODULE_LIST_LOCATION);
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


    static String firstCharUp(String s)
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



}
