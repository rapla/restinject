package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import javax.ws.rs.Path;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;

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
            DefaultImplementationRepeatable.class, Path.class };

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
        try
        {
            File f = getFile();
            if (!annotations.isEmpty())
            {
                processGwt(f, roundEnv);
            }
            //            if(roundEnv.processingOver())
            //            {
            //                createJavaFile(f);
            //            }
        }
        catch (IOException ioe)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, ioe.getMessage());
        }
        return true;
    }

    private boolean processGwt(File f, RoundEnvironment roundEnv) throws IOException
    {

        //        List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.client });
        for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
        {
            TypeElement implementationElement = (TypeElement) elem;
            DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
            TypeElement interfaceElement = getDefaultImplementationOf(annotation);
            final String qualifiedName = interfaceElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            addServiceFile(interfaceElement, implementationElement, f);
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
            }
        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(ExtensionPoint.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final String qualifiedName = typeElement.getQualifiedName().toString();
            appendToServiceList(f, qualifiedName);
            addServiceFile(typeElement, null, f);
        }

        for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final Extension annotation = typeElement.getAnnotation(Extension.class);
            TypeElement provider = getProvides(annotation);
            addServiceFile(provider, typeElement, f);
        }
        for (Element elem : roundEnv.getElementsAnnotatedWith(ExtensionRepeatable.class))
        {
            TypeElement typeElement = (TypeElement) elem;
            final ExtensionRepeatable annotation = typeElement.getAnnotation(ExtensionRepeatable.class);
            for (Extension extension : annotation.value())
            {
                TypeElement provider = getProvides(extension);
                addServiceFile(provider, typeElement, f);
            }
        }
        // Path
        appendToServiceList(f, Path.class.getCanonicalName());
        for (Element elem : roundEnv.getElementsAnnotatedWith(Path.class))
        {
            if(elem instanceof TypeElement)
            {
                TypeElement typeElement = (TypeElement) elem;
                addServiceFile(Path.class.getCanonicalName(), typeElement, f);
            }
        }
        return true;
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
        final File folder = allserviceList.getParentFile();
        String implementationName = implementationElement != null ? implementationElement.getQualifiedName().toString() : null;
        final File serviceFile = new File(folder, "services/" + serviceFileName);
        serviceFile.getParentFile().mkdirs();
        if (implementationElement == null)
        {
            appendToFile(serviceFile, null);
        }
        else
        {
            appendToFile(serviceFile, implementationName);
        }
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

    private File getFile() throws IOException
    {
        final Filer filer = processingEnv.getFiler();
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
