package org.rapla.inject.generator;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class RaplaGwtModuleProcessor
{

    ProcessingEnvironment processingEnv;
    RaplaGwtModuleProcessor.MyLogger logger;
    RaplaGwtModuleProcessor(ProcessingEnvironment processingEnv)
    {
        this.processingEnv = processingEnv;

        logger = new RaplaGwtModuleProcessor.MyLogger()
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
        PrintWriter src = null;
        try
        {
            Class classType = null;
            src = getSourceWriter( packageName, className);
            src.println("public void configure(GinBinder binder) {");
            String folder = AnnotationInjectionProcessor.GWT_MODULE_LIST;
            File f = AnnotationInjectionProcessor.getFile(processingEnv.getFiler());
            BufferedReader reader = new BufferedReader(new FileReader(f ));
            Set<String> interfaces = new LinkedHashSet<String>();
            for (String line = reader.readLine();line != null;)
            {
                interfaces.add(line);
            }
            reader.close();


            for (String interfaceName : interfaces)
            {
                Class<?> interaceClazz = null;
                try
                {
                    interaceClazz = Class.forName(interfaceName);

//                    addImplementations(interaceClazz, src, logger);
                }
                catch (ClassNotFoundException e1)
                {
                    logger.warn("Found interfaceName definition but no class for " + interfaceName);
                }
            }
            src.println("}");
            src.println("}");
            logger.info("Generating for: " + classType + " ");
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

    private boolean isImplementing(Class<?> interfaceClass, DefaultImplementation... clazzAnnot)
    {
        for (DefaultImplementation ext : clazzAnnot)
        {
            final Class<?> provides = ext.of();
            final InjectionContext[] contexts = ext.context();
            if (provides.equals(interfaceClass) && isRelevant(contexts))
            {
                return true;
            }
        }
        return false;
    }

    public static Collection<String> getImplementingIds(Class<?> interfaceClass, Extension... clazzAnnot)
    {
        Set<String> ids = new LinkedHashSet<>();
        for (Extension ext : clazzAnnot)
        {
            final Class<?> provides = ext.provides();
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

    /*
    private <T> void addImplementations(TypeElement interfaceClass, PrintWriter src,MyLogger logger ) throws IOException
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

        final String folder = "META-INF/services/";
        boolean foundExtension = false;
        boolean foundDefaultImpl = false;
        // load all implementations or extensions from service list file
        Set<String> implemantations = new LinkedHashSet<String>();

        File modules = AnnotationInjectionProcessor.getFile( line, f);
        final BufferedReader br = new BufferedReader(new FileReader(modules));
        String module = null;
        while ((module = br.readLine()) != null)
        {
            interfaces.add(module);
        }
        br.close();
        final Collection<URL> resources = find(classLoader,folder + interfaceName);
        for (URL url : resources)
        {
            //final URL def = moduleDefinition.nextElement();
            final InputStream in = url.openStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String implementationClassName = null;
            boolean implOrExtensionFound = false;
            while ((implementationClassName = reader.readLine()) != null)
            {
                try
                {
                    if (implemantations.contains(implementationClassName))
                    {
                        continue;
                    }
                    else
                    {
                        implemantations.add(implementationClassName);
                    }
                    // load class for implementation or extension
                    final Class<?> clazz = Class.forName(implementationClassName);
                    final Extension[] extensions = clazz.getAnnotationsByType(Extension.class);
                    Collection<String> idList = getImplementingIds(interfaceClass, extensions);

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
                            logger.warn(clazz + " provides no extension for " + interfaceName + " but is in the service list of " + interfaceName
                                    + ". You may need run a clean build.");
                        }
                    }

                    final DefaultImplementation[] defaultImplementations = clazz.getAnnotationsByType(DefaultImplementation.class);
                    final boolean implementing = isImplementing(interfaceClass, defaultImplementations);
                    if (implementing)
                    {
                        foundDefaultImpl = true;
                        Object singletonAnnotation = interfaceClass.getAnnotation(Singleton.class);
                        String sourceLine = "binder.bind(" + interfaceName + ".class).to(" + implementationClassName + ".class)";
                        if(singletonAnnotation != null)
                        {
                            sourceLine += ".in(Singleton.class)";
                        }
                        sourceLine+=";";
                        src.println(sourceLine);
                    }

                }
                catch (ClassNotFoundException e)
                {
                    logger.warn("Error loading implementationClassName (" + implementationClassName + ") for " + interfaceName
                            + " it maybe renamed/removed or the annotations have changed.");
                }

            }
            reader.close();
        }

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
                logger.warn("No DefaultImplemenation found for " + interfaceName + " Interface will not be available in the supported context gwt ");
            }
        }
    }
    */

    public PrintWriter getSourceWriter(String packageName, String simpleName) throws IOException
    {
        Filer filer = processingEnv.getFiler();
        JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + simpleName + ".java");
        PrintWriter writer = new PrintWriter(sourceFile.openWriter());
        writer.println("package " + packageName + ";");
        writer.println("import javax.inject.Singleton;");
        writer.println("import com.google.gwt.inject.client.GinModule;");
        writer.println("import com.google.gwt.inject.client.binder.GinBinder;");
        writer.println("import com.google.gwt.inject.client.multibindings.GinMultibinder;");
        writer.println("import com.google.gwt.inject.client.multibindings.GinMapBinder;");
        writer.println("class " + simpleName + " implements GinModule {");
        return writer;
    }

}
