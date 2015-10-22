package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.annotation.ProxyCreator;
import org.rapla.gwtjsonrpc.annotation.SourceWriter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.InjectionContext;

public class RaplaGwtModuleGenerator
{

    ProcessingEnvironment processingEnv;
    RaplaGwtModuleGenerator.MyLogger logger;

    public RaplaGwtModuleGenerator(ProcessingEnvironment processingEnv)
    {
        this.processingEnv = processingEnv;

        logger = new RaplaGwtModuleGenerator.MyLogger()
        {
            @Override public void warn(String message)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
            }

            @Override public void info(String message)
            {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);

            }
        };
    }

    public void process(String packageName, String className) throws IOException
    {
        SourceWriter src = null;
        try
        {
            //Class classType = null;
            src = getSourceWriter(packageName, className);
            src.println("public void configure(GinBinder binder) {");

            Set<String> interfaces = new LinkedHashSet<String>();
            final File f = AnnotationInjectionProcessor.readInterfacesInto(interfaces, processingEnv);

            for (String interfaceName : interfaces)
            {
                TypeElement interaceClazz = processingEnv.getElementUtils().getTypeElement(interfaceName);
                if (interaceClazz != null)
                {
                    addImplementations(interaceClazz, src, logger, f);
                }
                else
                {
                    logger.warn("Found interfaceName definition but no class for " + interfaceName);
                }
            }
            src.println("}");
            src.println("}");
        }
        finally
        {
            if (src != null)
            {
                src.close();
            }
        }
    }

    //
    //    private Collection<URL> find(ClassLoader classLoader,String fileWithfolder) throws IOException
    //    {
    //        List<URL> result = new ArrayList<URL>();
    //        Enumeration<URL> resources = classLoader.getResources(fileWithfolder);
    //        while (resources.hasMoreElements())
    //        {
    //            result.add(resources.nextElement());
    //        }
    //        return result;
    //    }

    protected boolean isRelevant(InjectionContext... context)
    {
        return InjectionContext.isInjectableOnGwt(context);
    }

    private boolean isImplementing(TypeElement interfaceClass, TypeElement implementingClass)
    {
        List<DefaultImplementation> exts = new ArrayList<DefaultImplementation>();
        DefaultImplementation di = implementingClass.getAnnotation(DefaultImplementation.class);
        if(di != null)
            exts.add(di);
        final DefaultImplementationRepeatable dir = implementingClass.getAnnotation(DefaultImplementationRepeatable.class);
        if(dir != null)
        {
            final DefaultImplementation[] value = dir.value();
            exts.addAll(Arrays.asList(value));
        }
        for(DefaultImplementation ext : exts)
        {
            final TypeElement provides = getDefaultImplementationOf(ext);
            final InjectionContext[] contexts = ext.context();
            if (provides.equals(interfaceClass) && isRelevant(contexts))
            {
                return true;
            }
        }
        return false;
    }

    public  Collection<String> getImplementingIds(TypeElement interfaceClass, TypeElement implementingClass)
    {
        List<Extension> exts = new ArrayList<Extension>();
        Extension di = implementingClass.getAnnotation(Extension.class);
        if(di != null)
            exts.add(di);
        final ExtensionRepeatable dir = implementingClass.getAnnotation(ExtensionRepeatable.class);
        if(dir != null)
        {
            final Extension[] value = dir.value();
            exts.addAll(Arrays.asList(value));
        }
        List<String> ids = new ArrayList<String>();
        for (Extension ext : exts)
        {
            final TypeElement provides = getProvides(ext);
            if (provides.equals(interfaceClass))
            {
                String id = ext.id();
                ids.add(id);
            }
        }
        return ids;

    }

    public interface MyLogger
    {
        void warn(String message);

        void info(String message);
    }

    private TypeElement getContext(ExtensionPoint annotation)
    {
        try
        {
            annotation.context(); // this should throw
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

    private <T> void addImplementations(TypeElement interfaceClass, SourceWriter src, MyLogger logger, File f) throws IOException
    {

        String interfaceName = interfaceClass.getQualifiedName().toString();
        final ExtensionPoint extensionPointAnnotation = interfaceClass.getAnnotation(ExtensionPoint.class);
        final boolean isExtensionPoint = extensionPointAnnotation != null;
        if (isExtensionPoint)
        {
            final InjectionContext[] context = extensionPointAnnotation.context();
            if (!isRelevant(context))
            {
                return;
            }
        }

        boolean foundExtension = false;
        boolean foundDefaultImpl = false;
        // load all implementations or extensions from service list file
        //Set<String> implemantations = new LinkedHashSet<String>();
        File modules = AnnotationInjectionProcessor.getFile(interfaceName, f);
        final BufferedReader br = new BufferedReader(new FileReader(modules));
        for (String implementationClassName = br.readLine(); implementationClassName != null; implementationClassName = br.readLine())
        {

            if ( interfaceClass.getAnnotation(RemoteJsonMethod.class) != null)
            {
                //final DefaultImplementation annotation = implementingType.getAnnotation(DefaultImplementation.class);
                //final InjectionContext[] context = annotation.context();
                if(implementationClassName.endsWith(ProxyCreator.PROXY_SUFFIX))
                {
                    String sourceLine = "binder.bind(" + interfaceName + ".class).to(" + implementationClassName + ".class)";
                    sourceLine += ";";
                    src.println(sourceLine);
                    foundDefaultImpl = true;
                }
                continue;
            }
            final TypeElement implementingType = processingEnv.getElementUtils().getTypeElement(implementationClassName);

            if (implementingType == null)
            {

                logger.warn("Error loading implementationClassName (" + implementationClassName + ") for " + interfaceName
                        + " it maybe renamed/removed or the annotations have changed.");
                continue;
            }
            Collection<String> idList = getImplementingIds(interfaceClass, implementingType);
            if (idList.size() > 0)
            {
                foundExtension = true;
                src.println("{");
                src.println("  GinMultibinder<" + interfaceName + "> setBinder = GinMultibinder.newSetBinder(binder, " + interfaceName + ".class);");
                src.println("  setBinder.addBinding().to(" + implementationClassName + ".class).in(Singleton.class);");
                src.println("  GinMapBinder<String," + interfaceName + "> mapBinder = GinMapBinder.newMapBinder(binder,String.class, " + interfaceName
                        + ".class);");
                for (String id : idList)
                {
                    src.println("  mapBinder.addBinding(\"" + id + "\").to(" + implementationClassName + ".class).in(Singleton.class);");
                }
                src.println("}");
            }
            else
            {
                if (isExtensionPoint)
                {
                    logger.warn(implementationClassName + " provides no extension for " + interfaceName + " but is in the service list of " + interfaceName
                            + ". You may need run a clean build.");
                }
            }

            final boolean implementing = isImplementing(interfaceClass, implementingType);
            if (implementing)
            {
                foundDefaultImpl = true;
                Object singletonAnnotation = interfaceClass.getAnnotation(Singleton.class);
                String sourceLine = "binder.bind(" + interfaceName + ".class).to(" + implementationClassName + ".class)";
                if (singletonAnnotation != null)
                {
                    sourceLine += ".in(Singleton.class)";
                }
                sourceLine += ";";
                src.println(sourceLine);
            }
        }
        br.close();
        if (isExtensionPoint)
        {
            if (!foundExtension)
            {
                src.println("GinMultibinder.newSetBinder(binder, " + interfaceName + ".class);");
                src.println("GinMultibinder.newMapBinder(binder, String.class," + interfaceName + ".class);");
            }
        }
        else
        {
            if (!foundDefaultImpl)
            {
                logger.warn("No DefaultImplementation found for " + interfaceName + " Interface will not be available in the supported context gwt ");
            }
        }
    }

    public  SourceWriter getSourceWriter(String packageName, String simpleName) throws IOException
    {
        Filer filer = processingEnv.getFiler();
        JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + simpleName );
        SourceWriter writer = new SourceWriter(sourceFile.openWriter());
        writer.println("package " + packageName + ";");
        writer.println("import javax.inject.Singleton;");
        writer.println("import com.google.gwt.inject.client.GinModule;");
        writer.println("import com.google.gwt.inject.client.binder.GinBinder;");
        writer.println("import com.google.gwt.inject.client.multibindings.GinMultibinder;");
        writer.println("import com.google.gwt.inject.client.multibindings.GinMapBinder;");
        writer.println("class " + simpleName + " implements GinModule {");
        return writer;
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

}
