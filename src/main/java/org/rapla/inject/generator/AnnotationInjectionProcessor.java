package org.rapla.inject.generator;

import org.rapla.inject.*;
import org.rapla.inject.generator.internal.DaggerModuleCreator;
import org.rapla.jsonrpc.generator.internal.GwtProxyCreator;
import org.rapla.jsonrpc.generator.internal.JavaClientProxyCreator;
import org.rapla.jsonrpc.generator.internal.TreeLogger;
import org.rapla.jsonrpc.common.RemoteJsonMethod;

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
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import javax.ws.rs.Path;
import java.io.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by Christopher on 07.09.2015.
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
            DefaultImplementationRepeatable.class, Path.class, RemoteJsonMethod.class };

    @Override public synchronized void init(ProcessingEnvironment processingEnv)
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
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (roundEnv.processingOver() || annotations.isEmpty())
        {
            return false;
        }
        try
        {
            processGwt(roundEnv);
        }
        catch (Exception ioe)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ioe.getMessage());
            return false;
        }
        return true;
    }

    private boolean processGwt( RoundEnvironment roundEnv) throws Exception
    {

        File f = getFile(processingEnv.getFiler());
        boolean found = false;
        TreeLogger proxyLogger = new TreeLogger();
        final Set<? extends Element> remoteMethods = roundEnv.getElementsAnnotatedWith(RemoteJsonMethod.class);
        for ( Element element : remoteMethods)
        {
            final TypeElement interfaceElement = (TypeElement) element;
            if(interfaceElement.getKind() != ElementKind.INTERFACE)
            {
                continue;
            }

            GwtProxyCreator proxyCreator = new GwtProxyCreator(interfaceElement, processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
            String proxyClassName = proxyCreator.create(proxyLogger);

            JavaClientProxyCreator swingProxyCreator = new JavaClientProxyCreator(interfaceElement, processingEnv, AnnotationInjectionProcessor.class.getCanonicalName());
            String swingproxyClassName = swingProxyCreator.create(proxyLogger);

            final String qualifiedName = interfaceElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            final File serviceFile = getFile(qualifiedName, f);
            appendToFile(serviceFile, proxyClassName);
            appendToFile(serviceFile, swingproxyClassName );
            if (interfaceElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }
/*
        for (Element elem : roundEnv.getElementsAnnotatedWith(RequestScoped.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            TypeElement interfaceElement = processingEnv.getElementUtils().getTypeElement(RequestScoped.class.getCanonicalName());
            final String qualifiedName = interfaceElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            addServiceFile(interfaceElement, implementationElement, f);
            if (implementationElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }
        */

        //        List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.gwt });
        for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
            TypeElement interfaceElement = getDefaultImplementationOf(annotation);
            final String qualifiedName = interfaceElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            addServiceFile(interfaceElement, implementationElement, f);
            if (implementationElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementationRepeatable.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            DefaultImplementationRepeatable annotation = elem.getAnnotation(DefaultImplementationRepeatable.class);
            for (DefaultImplementation defaultImplementation : annotation.value())
            {
                TypeElement interfaceElement = getDefaultImplementationOf(defaultImplementation);
                final String qualifiedName = interfaceElement.getQualifiedName().toString();
                appendToServiceList(f, qualifiedName);
                addServiceFile(interfaceElement, implementationElement, f);
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
            addServiceFile(typeElement, null, f);
            found = true;
        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final Extension annotation = typeElement.getAnnotation(Extension.class);
            TypeElement provider = getProvides(annotation);
            addServiceFile(provider, typeElement, f);
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
                TypeElement provider = getProvides(extension);
                addServiceFile(provider, typeElement, f);
            }
            if (typeElement.getAnnotation(Generated.class) == null)
            {
                found = true;
            }
        }

        // Path
        boolean append = false;
        for (Element elem : roundEnv.getElementsAnnotatedWith(Path.class))
        {
            if(elem instanceof TypeElement)
            {
                TypeElement typeElement = (TypeElement) elem;
                addServiceFile(Path.class.getCanonicalName(), typeElement, f);
                append = true;
            }
        }
        if(append)
        {
            appendToServiceList(f, Path.class.getCanonicalName());
            found = true;
       }
        // only generate the modules if we processed a class
        if ( found)
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

    private void addServiceFile(TypeElement interfaceElement, TypeElement implementationElement, File allserviceList) throws IOException
    {
        final String serviceFileName = interfaceElement.getQualifiedName().toString();
        addServiceFile(serviceFileName, implementationElement, allserviceList);
        
    }
    private void addServiceFile(String serviceFileName, TypeElement implementationElement, File allserviceList) throws IOException
    {
        final File serviceFile = getFile(serviceFileName, allserviceList);
        String implementationName = implementationElement != null ? implementationElement.getQualifiedName().toString() : null;
        if (implementationElement == null)
        {
            appendToFile(serviceFile, null);
        }
        else
        {
            appendToFile(serviceFile, implementationName);
        }
    }
    
    public static Set<String> readFileContent(String serviceFileName, File allserviceList) throws FileNotFoundException, IOException
    {
        final File file = getFile(serviceFileName, allserviceList);
        Set<String> collection = new LinkedHashSet<String>();
        readInto(collection , file);
        return collection;
    }

    static public File getFile(String serviceFileName, File allserviceList)
    {
        final File folder = allserviceList.getParentFile();
        final File serviceFile = new File(folder, "services/" + serviceFileName);
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

    /**
     * Reads the interfaces defined in META-INF/org.rapla.servicelist into the provided set
     * and returns the File.
     */
    public static File readInterfacesInto(Set<String> interfaces, ProcessingEnvironment processingEnvironment) throws IOException, FileNotFoundException
    {
        File f = AnnotationInjectionProcessor.getFile(processingEnvironment.getFiler());
        readInto(interfaces, f);
        return f;
    }

    private static void readInto(Set<String> interfaces, File f) throws FileNotFoundException, IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        for (String line = reader.readLine(); line != null;line = reader.readLine() )
        {
            interfaces.add(line);
        }
        reader.close();
    }

    public static File getFile(Filer filer) throws IOException
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
