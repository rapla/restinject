package org.rapla.dagger;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.rapla.gwtjsonrpc.annotation.ProxyCreator;
import org.rapla.gwtjsonrpc.annotation.SourceWriter;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.DefaultImplementationRepeatable;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionRepeatable;
import org.rapla.inject.generator.AnnotationInjectionProcessor;

import dagger.Module;

public class DaggerModuleProcessor
{

    private final ProcessingEnvironment processingEnvironment;

    public DaggerModuleProcessor(ProcessingEnvironment processingEnvironment)
    {
        super();
        this.processingEnvironment = processingEnvironment;
    }

    public void process() throws Exception
    {
        generateModuleClass();
    }

    private void generateModuleClass() throws Exception
    {
        final JavaFileObject source = processingEnvironment.getFiler().createSourceFile("org.rapla.dagger.DaggerGwtModule");
        final SourceWriter moduleWriter = new SourceWriter(source.openOutputStream());
        moduleWriter.println("package org.rapla.dagger;");
        moduleWriter.println();
        moduleWriter.println("@" + Module.class.getCanonicalName());
        moduleWriter.println("public class DaggerGwtModule {");
        moduleWriter.indent();
        Set<String> interfaces = new LinkedHashSet<String>();
        final File allserviceList = AnnotationInjectionProcessor.readInterfacesInto(interfaces, processingEnvironment);
        for (String interfaceName : interfaces)
        {
            createMethods(interfaceName, moduleWriter, allserviceList);
        }
        moduleWriter.outdent();
        moduleWriter.println("}");
        moduleWriter.close();
    }

    private void createMethods(String interfaceName, SourceWriter moduleWriter, File allserviceList) throws Exception
    {
        final Set<String> implementingClasses = AnnotationInjectionProcessor.readFileContent(interfaceName, allserviceList);
        for (String implementingClass : implementingClasses)
        {
            final TypeElement implementingClassTypeElement = processingEnvironment.getElementUtils().getTypeElement(implementingClass);
            if (implementingClassTypeElement == null)
            {// Generated Json Proxies
                if (implementingClass.endsWith(ProxyCreator.PROXY_SUFFIX))
                {
                    String interaceNameWithoutPackage = extractNameWithoutPackage(interfaceName);
                    final String implementingClassWithoutPackage = extractNameWithoutPackage(implementingClass);
                    moduleWriter.println("public " + interfaceName + " provide_" + interaceNameWithoutPackage + "_" + implementingClassWithoutPackage + "() {");
                    moduleWriter.indent();
                    moduleWriter.println("return new " + implementingClass + "();");
                    moduleWriter.outdent();
                    moduleWriter.println("}");
                }
            }
            else
            {
                final DefaultImplementationRepeatable defaultImplementationRepeatable = implementingClassTypeElement
                        .getAnnotation(DefaultImplementationRepeatable.class);
                if (defaultImplementationRepeatable != null)
                {
                    final DefaultImplementation[] defaultImplementations = defaultImplementationRepeatable.value();
                    for (DefaultImplementation defaultImplementation : defaultImplementations)
                    {
                        generate(implementingClassTypeElement, interfaceName, defaultImplementation, moduleWriter);
                    }
                }
                final DefaultImplementation defaultImplementation = implementingClassTypeElement.getAnnotation(DefaultImplementation.class);
                if (defaultImplementation != null)
                {
                    generate(implementingClassTypeElement, interfaceName, defaultImplementation, moduleWriter);
                }
                final ExtensionRepeatable extensionRepeatable = implementingClassTypeElement.getAnnotation(ExtensionRepeatable.class);
                if (extensionRepeatable != null)
                {
                    final Extension[] extensions = extensionRepeatable.value();
                    for (Extension extension : extensions)
                    {
                        generate(implementingClassTypeElement, interfaceName, extension, moduleWriter);
                    }
                }
                final Extension extension = implementingClassTypeElement.getAnnotation(Extension.class);
                if (extension != null)
                {
                    generate(implementingClassTypeElement, interfaceName, extension, moduleWriter);
                }
            }
        }
    }

    private void generate(TypeElement implementingClassTypeElement, String interfaceName, DefaultImplementation defaultImplementation,
            SourceWriter moduleWriter)
    {
        final TypeElement defaultImplementationOf = getDefaultImplementationOf(defaultImplementation);
        if (defaultImplementation != null
                && processingEnvironment.getTypeUtils().isAssignable(implementingClassTypeElement.asType(), defaultImplementationOf.asType()))
        {
            final String defaultImplClassName = implementingClassTypeElement.getSimpleName().toString();
            final String interaceNameWithoutPackage = extractNameWithoutPackage(interfaceName);
            moduleWriter.println("public " + interfaceName + " provide_" + interaceNameWithoutPackage + "_" + defaultImplClassName + "() {");
            moduleWriter.indent();
            moduleWriter.println("return new " + implementingClassTypeElement.getQualifiedName().toString() + "();");
            moduleWriter.outdent();
            moduleWriter.println("}");
        }
    }

    private void generate(TypeElement implementingClassTypeElement, String interfaceName, Extension extension, SourceWriter moduleWriter)
    {
        // TODO Auto-generated method stub

    }

    private static String extractNameWithoutPackage(String className)
    {
        final int lastIndexOf = className.lastIndexOf(".");
        if (lastIndexOf >= 0)
        {
            return className.substring(lastIndexOf + 1);
        }
        return className;
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
        Types TypeUtils = processingEnvironment.getTypeUtils();
        return (TypeElement) TypeUtils.asElement(typeMirror);
    }
}
