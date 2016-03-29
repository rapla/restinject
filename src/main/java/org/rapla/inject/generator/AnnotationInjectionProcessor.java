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

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
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
import org.rapla.inject.generator.internal.DaggerModuleCreator;
import org.rapla.jsonrpc.generator.internal.GwtProxyCreator;
import org.rapla.jsonrpc.generator.internal.JavaClientProxyCreator;
import org.rapla.jsonrpc.generator.internal.TreeLogger;

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
    public static final String GWT_MODULE_LIST = "META-INF/org.rapla.servicelist";

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.latestSupported();
    }

    Class<?>[] supportedAnnotations = new Class[] { Extension.class, ExtensionRepeatable.class, ExtensionPoint.class, DefaultImplementation.class,
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

    @Override
    synchronized public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (roundEnv.processingOver() || annotations.isEmpty())
        {
            return false;
        }
        try
        {
            process(roundEnv);
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

    private boolean process(RoundEnvironment roundEnv) throws Exception
    {
        File f = getInterfaceList(processingEnv.getFiler());
        File folder = f.getParentFile();
        boolean found = false;
        TreeLogger proxyLogger = new TreeLogger();
        /*
        final Set<? extends Element> remoteMethods = roundEnv.getElementsAnnotatedWith(RemoteJsonMethod.class);
        for (Element element : remoteMethods)
        {
            final TypeElement interfaceElement = (TypeElement) element;
            if (interfaceElement.getKind() != ElementKind.INTERFACE)
            {
                continue;
            }
            GwtProxyCreator proxyCreator = new GwtProxyCreator(interfaceElement, processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
            String proxyClassName = proxyCreator.create(proxyLogger);

            JavaClientProxyCreator swingProxyCreator = new JavaClientProxyCreator(interfaceElement, processingEnv,
                    AnnotationInjectionProcessor.class.getCanonicalName());
            String swingproxyClassName = swingProxyCreator.create(proxyLogger);

            final String qualifiedName = interfaceElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            final File serviceFile = getFile(f.getParentFile(), "services/" + qualifiedName);
            appendToFile(serviceFile, proxyClassName);
            appendToFile(serviceFile, swingproxyClassName);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Proxies " + proxyClassName + ", " + swingproxyClassName);
            if (interfaceElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }
        */
        /*
        for (Element elem : roundEnv.getElementsAnnotatedWith(RequestScoped.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            TypeElement interfaceElement = processingEnv.getElementUtils().getTypeElement(RequestScoped.class.getCanonicalName());
            final String qualifiedName = interfaceElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            appendToFile(interfaceElement, implementationElement, f);
            if (implementationElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }
        */
        boolean pathAnnotationFound = false;

        //        List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.gwt });
        for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
            TypeElement interfaceElement = getDefaultImplementationOf(annotation);
            if(interfaceElement.getAnnotation(Path.class) != null)
            {
                if (interfaceElement.getKind() != ElementKind.INTERFACE)
                {
                    continue;
                }
                GwtProxyCreator proxyCreator = new GwtProxyCreator(interfaceElement, processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
                String proxyClassName = proxyCreator.create(proxyLogger);

                JavaClientProxyCreator swingProxyCreator = new JavaClientProxyCreator(interfaceElement, processingEnv,
                        AnnotationInjectionProcessor.class.getCanonicalName());
                String swingproxyClassName = swingProxyCreator.create(proxyLogger);

                {
                    final String qualifiedName = interfaceElement.getQualifiedName().toString();
                    appendToServiceList(f, qualifiedName);
                    final File serviceFile = getFile(f.getParentFile(), "services/" + qualifiedName);
                    appendToFile(serviceFile, proxyClassName);
                    appendToFile(serviceFile, swingproxyClassName);
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Proxies " + proxyClassName + ", " + swingproxyClassName);
                    if (interfaceElement.getAnnotation(Generated.class) == null)
                    {
                        found = true;
                    }
                }
                {
                    final File serviceFile = getFile(f.getParentFile(), "services/" + Path.class.getCanonicalName());
                    appendToFile(serviceFile, implementationElement.getQualifiedName().toString());
                    pathAnnotationFound = true;
                }
            }
            {
                final String qualifiedName = interfaceElement.getQualifiedName().toString();
                appendToServiceList(f, qualifiedName);
                addServiceFile(interfaceElement, implementationElement, folder);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding DefaultImplemenation " + implementationElement);
                if (implementationElement.getAnnotation(Generated.class) == null)
                {
                    found = true;
                }
            }
        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementationRepeatable.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            DefaultImplementationRepeatable annotation = elem.getAnnotation(DefaultImplementationRepeatable.class);
            for (DefaultImplementation defaultImplementation : annotation.value())
            {
                TypeElement interfaceElement = getDefaultImplementationOf(defaultImplementation);
                final String interfaceQualifiedName = interfaceElement.getQualifiedName().toString();
                appendToServiceList(f, interfaceQualifiedName);
                addServiceFile(interfaceElement, implementationElement, folder);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding DefaultImplemenation " + implementationElement);
                if (implementationElement.getAnnotation(Generated.class) == null)
                {
                    found = true;
                }
            }

        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(ExtensionPoint.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final String qualifiedName = typeElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            addServiceFile(typeElement, null, folder);
            found = true;
        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final Extension annotation = typeElement.getAnnotation(Extension.class);
            TypeElement extensionPoint = getProvides(annotation);
            final String interfaceQualifiedName = extensionPoint.getQualifiedName().toString();
            appendToServiceList(f, interfaceQualifiedName);
            addServiceFile(extensionPoint, typeElement, folder);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding extension " + typeElement);
            if (typeElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }

        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(ExtensionRepeatable.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final ExtensionRepeatable annotation = typeElement.getAnnotation(ExtensionRepeatable.class);
            final Extension[] value = annotation.value();
            for (Extension extension : value)
            {
                TypeElement extensionPoint = getProvides(extension);
                final String interfaceQualifiedName = extensionPoint.getQualifiedName().toString();
                appendToServiceList(f, interfaceQualifiedName);
                addServiceFile(extensionPoint, typeElement, folder);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Adding extension " + typeElement);
            }
            if (typeElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }

        // Path
        for (Element elem : roundEnv.getElementsAnnotatedWith(Path.class))
        {
            if (elem instanceof TypeElement)
            {
                final TypeElement typeElement=(TypeElement) elem;
                if(typeElement.getKind() != ElementKind.INTERFACE)
                {
                    addServiceFile(Path.class.getCanonicalName(), typeElement, folder);
                    pathAnnotationFound = true;
                }
            }
        }
        if (pathAnnotationFound)
        {
            appendToServiceList(f, Path.class.getCanonicalName());
            found = true;
        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(Provider.class))
        {
            if (elem instanceof TypeElement)
            {
                addServiceFile(Provider.class.getCanonicalName(), (TypeElement) elem, folder);
            }
        }
        // only generate the modules if we processed a class
        if (found)
        {
            //            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating RaplaGinModulesGenerated");
            //            String className = "RaplaGinModulesGenerated";
            //            String packageName = "org.rapla.inject.client.gwt";
            //            final RaplaGwtModuleGenerator raplaGwtModuleProcessor = new RaplaGwtModuleGenerator(processingEnv);
            //            raplaGwtModuleProcessor.process(packageName, className);

            // Dagger
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating Dagger Modules");
            final DaggerModuleCreator daggerModuleProcessor = new DaggerModuleCreator(processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
            daggerModuleProcessor.process();
        }

        return found;
    }

    private void appendToServiceList(File serviceListFile, String interfaceName) throws IOException
    {
        appendToFile(serviceListFile, interfaceName);
    }

    private void addServiceFile(TypeElement interfaceElement, TypeElement implementationElement, File folder) throws IOException
    {
        final String serviceFileName = interfaceElement.getQualifiedName().toString();
        addServiceFile(serviceFileName, implementationElement, folder);

    }

    private void addServiceFile(String serviceFileName, TypeElement implementationElement, File folder) throws IOException
    {
        String implementationName = implementationElement != null ? implementationElement.getQualifiedName().toString() : null;
        appendToFile("services/" + serviceFileName, folder, implementationName);
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
        JavaFileManager.Location location = StandardLocation.SOURCE_OUTPUT;

        File f;
        try
        {
            FileObject resource = filer.getResource(location, pkg, GWT_MODULE_LIST);
            f = new File(resource.toUri());
        }
        catch (IOException ex)
        {
            FileObject resource = filer.createResource(location, pkg, GWT_MODULE_LIST);
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
