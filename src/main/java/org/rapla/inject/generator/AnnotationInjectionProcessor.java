package org.rapla.inject.generator;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

/**
 * Created by Christopher on 07.09.2015.
 */
public class AnnotationInjectionProcessor extends AbstractProcessor
{

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override public Set<String> getSupportedAnnotationTypes()
    {
        Set<String> supported = new HashSet<String>();
        supported.add(Extension.class.getCanonicalName());
        supported.add(ExtensionPoint.class.getCanonicalName());
        supported.add(DefaultImplementation.class.getCanonicalName());
        return supported;
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        return processGwt(annotations, roundEnv);
    }

    private boolean processGwt(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
            String packageName = "org.rapla.client.gwt";
            Writer writer = processingEnv.getFiler().createSourceFile(packageName + ".RaplaGwtModule").openWriter();
            PrintWriter w = new PrintWriter(writer);
            w.print("package " + packageName + ";\n");
            w.print("import javax.inject.Singleton;\n");
            w.print("import com.google.gwt.inject.client.GinModule;\n");
            w.print("import com.google.gwt.inject.client.binder.GinBinder;\n");
            w.print("import com.google.gwt.inject.client.multibindings.GinMultibinder;\n");
            w.print("public class RaplaGwtModule implements GinModule { \n");
            w.print("   public final void configure(GinBinder binder) {\n");

            List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] {InjectionContext.gwt,InjectionContext.client});

            for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class)) {
                TypeElement typeElement = (TypeElement) elem;
                TypeElement defaultImplementationOf = getDefaultImplementationOf(typeElement);
                InjectionContext[] context = typeElement.getAnnotation(DefaultImplementation.class).context();
                List<InjectionContext> contextList = new ArrayList<InjectionContext>();

                boolean notContains = Collections.disjoint(Arrays.asList(context), gwtContexts);
                if(context.length > 0 && notContains )
                {
                    continue;
                }
                Name qualifiedName = typeElement.getQualifiedName();
                w.print("binder.bind("+defaultImplementationOf+".class).to("+qualifiedName+".class).in(Singleton.class);\n");
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "Default Impl found in " + qualifiedName + " for  " + defaultImplementationOf + " contexts " + context);
            }

            Set<String> supportedExtenstionPoints = new HashSet<String>();
            for (Element elem : roundEnv.getElementsAnnotatedWith(ExtensionPoint.class)) {
                TypeElement typeElement = (TypeElement) elem;
                InjectionContext[] context = typeElement.getAnnotation(ExtensionPoint.class).context();
                List<InjectionContext> contextList = new ArrayList<InjectionContext>();

                boolean notContains = Collections.disjoint(Arrays.asList(context), gwtContexts);
                if(context.length > 0 && notContains )
                {
                    continue;
                }
                String extensionName = typeElement.getQualifiedName().toString();
                supportedExtenstionPoints.add( extensionName);
                w.print("   {\n");
                w.print("        GinMultibinder<" + extensionName + "> setBinder = GinMultibinder.newSetBinder(binder, " + extensionName + ".class);\n");
                w.print("   }\n");
            }

            for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class)) {

                TypeElement typeElement = (TypeElement) elem;
                TypeElement provider = getProvides(typeElement);
                Name qualifiedName = typeElement.getQualifiedName();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Provider found for " + qualifiedName);
                {
                    String extensionName = provider.getQualifiedName().toString();
                    if ( !supportedExtenstionPoints.contains( extensionName))
                    {
                        continue;
                    }
                    String extensionClassName = qualifiedName.toString();
                    w.print("   {\n");
                    w.print("        GinMultibinder<" + extensionName + "> setBinder = GinMultibinder.newSetBinder(binder, " + extensionName + ".class);\n");
                    w.print("        setBinder.addBinding().to("+extensionClassName+ ".class).in(Singleton.class);\n" );
                    w.print("   }\n");
                }
            }
            w.print("    }\n");
            w.print("}");
            w.close();
        }
        catch (IOException ioe)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,ioe.getMessage());
        }
        return true;
    }

    private boolean processSwing(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
            String packageName = "org.rapla";
            Writer writer = processingEnv.getFiler().createSourceFile(packageName + ".RaplaMainModule").openWriter();
            PrintWriter w = new PrintWriter(writer);
            w.print("package " + packageName + ";\n");
            w.print("public class RaplaMainModule implements org.rapla.framework.PluginDescriptor<org.rapla.client.ClientServiceContainer> { \n");
            w.print("   public final void provideServices(org.rapla.client.ClientServiceContainer container, org.rapla.framework.Configuration configuration) {\n");


            for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class)) {

                TypeElement typeElement = (TypeElement) elem;
                TypeElement provider = getProvides(typeElement);
                Name qualifiedName = typeElement.getQualifiedName();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Provider found for " + qualifiedName);
                //for (Class provider:provides)
                {
                    String extensionName = provider.getQualifiedName().toString();
                    String extensionClassName = qualifiedName.toString();
                    w.print("\ncontainer.addContainerProvidedComponent("+extensionName+".class,"+extensionClassName+ ".class);\n");
                }
            }
            w.print("    }\n");
            w.print("}");
            w.close();
        }
        catch (IOException ioe)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,ioe.getMessage());
        }
        return true;
    }

    private TypeElement getProvides(TypeElement elem) {
        Extension annotation = elem.getAnnotation(Extension.class);
        try
        {
            annotation.provides(); // this should throw
        }
        catch( MirroredTypeException mte )
        {
            return asTypeElement(mte.getTypeMirror());
        }
        return null; // can this ever happen ??
    }

    private TypeElement getExtensionPointContext(TypeElement elem) {
        ExtensionPoint annotation = elem.getAnnotation(ExtensionPoint.class);
        try
        {
            annotation.context(); // this should throw
        }
        catch( MirroredTypeException mte )
        {
            return asTypeElement(mte.getTypeMirror());
        }
        return null; // can this ever happen ??
    }

    private TypeElement getDefaultImplementationContext(TypeElement elem) {
        DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
        try
        {
            annotation.context(); // this should throw
        }
        catch( MirroredTypeException mte )
        {
            return asTypeElement(mte.getTypeMirror());
        }
        return null; // can this ever happen ??
    }

    private TypeElement getDefaultImplementationOf(TypeElement elem) {
        DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
        try
        {
            annotation.of(); // this should throw
        }
        catch( MirroredTypeException mte )
        {
            return asTypeElement(mte.getTypeMirror());
        }
        return null; // can this ever happen ??
    }

    private TypeElement asTypeElement(TypeMirror typeMirror) {
        Types TypeUtils = this.processingEnv.getTypeUtils();
        return (TypeElement)TypeUtils.asElement(typeMirror);
    }
}
