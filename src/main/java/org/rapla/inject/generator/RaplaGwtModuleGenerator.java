package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class RaplaGwtModuleGenerator extends Generator
{
    private static final Collection<InjectionContext> supportedContexts = new LinkedHashSet<InjectionContext>();
    ClassLoader classLoader;

    @Override public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException
    {
        try
        {
            supportedContexts.add(InjectionContext.gwt);
            supportedContexts.add(InjectionContext.client);
            JClassType classType;
            classType = context.getTypeOracle().getType(typeName);
            classLoader = Class.forName(typeName).getClassLoader();
            SourceWriter src = getSourceWriter(classType, context, logger);
            // if we already created getSourceWriter returns null so we can abort
            String nameOfGeneratedClass = typeName + "Generated";
            if ( src == null)
            {
                return  nameOfGeneratedClass;
            }
            src.println("public void configure(GinBinder binder) {");
            src.indent();
            String folder = AnnotationInjectionProcessor.GWT_MODULE_LIST;
            Set<String> interfaces = new LinkedHashSet<String>();
            final Collection<URL> resources = find(folder);
            for (URL url : resources)
            {
                final InputStream modules = url.openStream();
                final BufferedReader br = new BufferedReader(new InputStreamReader(modules, "UTF-8"));
                String module = null;
                while ((module = br.readLine()) != null)
                {
                    interfaces.add(module);
                }
                br.close();
            }
            for (String interfaceName : interfaces)
            {
                Class<?> interaceClazz = null;
                try
                {
                    interaceClazz = Class.forName(interfaceName);
                    addImplementations(interaceClazz, src, logger);
                }
                catch (ClassNotFoundException e1)
                {
                    logger.log(Type.WARN, "Found interfaceName definition but no class for " + interfaceName);
                }
            }
            src.outdent();
            src.println("}");
            src.commit(logger);
            logger.log(Type.INFO, "Generating for: " + typeName + " ");
            System.out.println("Generating for: " + typeName);
            return  nameOfGeneratedClass;
        }
        catch (Exception e)
        {
            logger.log(Type.ERROR, e.getMessage(), e);
            throw new UnableToCompleteException();
        }
    }

    private Collection<URL> find(String fileWithfolder) throws IOException
    {

        List<URL> result = new ArrayList<URL>();
        Enumeration<URL> resources = classLoader.getResources(fileWithfolder);
        while (resources.hasMoreElements())
        {
            result.add(resources.nextElement());
        }
        return result;
    }

    private boolean isRelevant(InjectionContext... context)
    {
        final List<InjectionContext> c2 = Arrays.asList(context);
        return !Collections.disjoint(c2, supportedContexts) || c2.contains(InjectionContext.all);
    }

    private boolean isImplementing(Class<?> interfaceClass, DefaultImplementation... clazzAnnot)
    {
        for (DefaultImplementation ext : clazzAnnot)
        {
            final Class<?> provides = ext.of();
            final InjectionContext[] context = ext.context();
            if (provides.equals(interfaceClass) && isRelevant(context))
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

    private <T> void addImplementations(Class<T> interfaceClass, SourceWriter src, TreeLogger logger) throws IOException
    {
        String interfaceName = interfaceClass.getCanonicalName();
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
        final Collection<URL> resources = find(folder + interfaceName);
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
                        src.indent();
                        src.println("GinMultibinder<" + interfaceName + "> setBinder = GinMultibinder.newSetBinder(binder, " + interfaceName + ".class);");
                        src.println("setBinder.addBinding().to(" + implementationClassName + ".class).in(Singleton.class);");
                        src.println("GinMapBinder<String," + interfaceName + "> mapBinder = GinMapBinder.newMapBinder(binder,String.class, " + interfaceName
                                + ".class);");
                        for (String id : idList)
                        {
                            src.println("mapBinder.addBinding(\"" + id + "\").to(" + implementationClassName + ".class).in(Singleton.class);");
                        }
                        src.outdent();
                        src.println("}");
                    }
                    else
                    {
                        if (isExtensionPoint)
                        {
                            logger.log(Type.WARN, clazz + " provides no extension for " + interfaceName + " but is in the service list of " + interfaceName
                                    + ". You may need run a clean build.");
                        }
                    }

                    final DefaultImplementation[] defaultImplementations = clazz.getAnnotationsByType(DefaultImplementation.class);
                    final boolean implementing = isImplementing(interfaceClass, defaultImplementations);
                    if (implementing)
                    {
                        foundDefaultImpl = true;
                        Singleton singletonAnnotation = interfaceClass.getAnnotation(Singleton.class);
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
                    logger.log(Type.WARN, "Error loading implementationClassName (" + implementationClassName + ") for " + interfaceName
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
                logger.log(Type.WARN,
                        "No DefaultImplemenation found for " + interfaceName + " Interface will not be available in the supported Contexts " + supportedContexts
                                + " ");
            }
        }
    }

    public SourceWriter getSourceWriter(JClassType classType, GeneratorContext context, TreeLogger logger)
    {
        String packageName = classType.getPackage().getName();
        String simpleName = classType.getSimpleSourceName() + "Generated";
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, simpleName);
        composer.addImplementedInterface("GinModule");
        composer.addImport(packageName + "." + classType.getSimpleSourceName());
        composer.addImport("javax.inject.Singleton");
        composer.addImport("com.google.gwt.inject.client.GinModule");
        composer.addImport("com.google.gwt.inject.client.binder.GinBinder");
        composer.addImport("com.google.gwt.inject.client.multibindings.GinMultibinder");
        composer.addImport("com.google.gwt.inject.client.multibindings.GinMapBinder");
        PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
        if (printWriter == null)
        {
            return null;
        }
        else
        {
            SourceWriter sw = composer.createSourceWriter(context, printWriter);
            return sw;
        }
    }

}
