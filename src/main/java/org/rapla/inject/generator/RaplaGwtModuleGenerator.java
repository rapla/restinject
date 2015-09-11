package org.rapla.inject.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

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
    private static final Collection<InjectionContext> supportedContexts = new ArrayList<InjectionContext>();

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException
    {
        try
        {
            supportedContexts.add(InjectionContext.gwt);
            supportedContexts.add(InjectionContext.client);
            JClassType classType;
            classType = context.getTypeOracle().getType(typeName);
            SourceWriter src = getSourceWriter(classType, context, logger);
            src.println("public void configure(GinBinder binder) {");
            src.indent();
            String folder = AnnotationInjectionProcessor.GWT_MODULE_LIST;
            if (!folder.startsWith("/"))
            {
                folder = "/" + folder;
            }
            final Enumeration<URL> resources = System.class.getClassLoader().getResources(folder);
            while (resources.hasMoreElements())
            {
                final InputStream modules = resources.nextElement().openStream();
                final BufferedReader br = new BufferedReader(new InputStreamReader(modules, "UTF-8"));
                String module = null;
                while ((module = br.readLine()) != null)
                {
                    importModule(module, src, logger);
                }
                br.close();
            }
            src.outdent();
            src.println("}");
            src.commit(logger);
            System.out.println("Generating for: " + typeName);
            return typeName + "Generated";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private void importModule(String module, SourceWriter src, TreeLogger logger)
    {
        Class<?> moduleClazz = null;
        try
        {
            moduleClazz = Class.forName(module);
        }
        catch (ClassNotFoundException e1)
        {
            logger.log(Type.WARN, "Found module definition but no class for " + module);
            return;
        }
        final List<Annotation> annotations = Arrays.asList(moduleClazz.getAnnotations());
        boolean supported = false;
        for (Annotation annotation : annotations)
        {
            if(annotation instanceof DefaultImplementation && !Collections.disjoint(supportedContexts, Arrays.asList(((DefaultImplementation)annotation).context())))
            {
                supported = true;
                DefaultImplementation defImplAnnotation = (DefaultImplementation)annotation;
                final Class<?> interfaceClass = defImplAnnotation.of();
                src.println("binder.bind("+interfaceClass.getCanonicalName()+".class).to("+moduleClazz.getCanonicalName()+".class).in(Singleton.class);");
            }
            else if (annotation instanceof ExtensionPoint)
            {
                final String folder = "/META-INF/services/";
                boolean foundImpl = false;
                try
                {
                    final Enumeration<URL> moduleDefinition = System.class.getClassLoader().getResources(folder + module);
                    while (moduleDefinition.hasMoreElements())
                    {
                        final URL def = moduleDefinition.nextElement();
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(def.openStream(), "UTF-8"));
                        String impl = null;
                        while ((impl = reader.readLine()) != null)
                        {
                            try
                            {
                                final Class<?> clazz = Class.forName(impl);
                                final Extension clazzAnnot = clazz.getAnnotation(Extension.class);
                                if(clazzAnnot != null)
                                {
                                    foundImpl = true;
                                    src.println("{");
                                    src.indent();
                                    src.println("GinMultibinder<" + module + "> setBinder = GinMultibinder.newSetBinder(binder, "+module+".class);");
                                    src.println("setBinder.addBinding().to(" + impl + ".class).in(Singleton.class);");
                                    src.outdent();
                                    src.println("}");
                                }
                                else
                                {
                                    logger.log(Type.WARN, "Found reference to (" + impl + ") for " + module);
                                }
                            }
                            catch (ClassNotFoundException e)
                            {
                                logger.log(Type.WARN, "Error loading impl (" + impl + ") for " + module, e);
                            }
                        }
                        reader.close();
                    }
                }
                catch (IOException e)
                {
                    logger.log(Type.ERROR, "Error loading module files for " + module, e);
                }
            }
            else if (annotation instanceof Extension){
                
            }
        }
        if (!supported)
        {
            logger.log(Type.WARN, "Found module definition but class has no supported annotation: " + module);
            return;
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
