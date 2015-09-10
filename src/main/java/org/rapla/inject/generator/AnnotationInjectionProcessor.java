package org.rapla.inject.generator;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.Extension;
import org.rapla.inject.ExtensionPoint;
import org.rapla.inject.InjectionContext;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;

/**
 * Created by Christopher on 07.09.2015.
 */
public class AnnotationInjectionProcessor extends AbstractProcessor
{

    private static class MyWriter
    {
        private final int EMPTY_PER_INDENT = 4;
        private int indent = 0;
        private final PrintWriter w;
        private final String fullName;
        public MyWriter(ProcessingEnvironment roundEnv, TypeElement typeElement,String suffix) throws IOException
        {
            String qualifiedName = typeElement.getQualifiedName().toString();
            String simpleName = typeElement.getSimpleName().toString();
            String packageName = qualifiedName.substring(0, qualifiedName.length() - (simpleName.length() + 1));
            String className = simpleName + suffix +"Module";
            fullName = packageName + "." + className;
            Writer writer = roundEnv.getFiler().createSourceFile(fullName).openWriter();
            w =new PrintWriter(writer);
            println("package " + packageName + ";");
            println("import javax.inject.Singleton;");
            println("import com.google.gwt.inject.client.GinModule;");
            println("import com.google.gwt.inject.client.binder.GinBinder;");
            println("import com.google.gwt.inject.client.multibindings.GinMultibinder;");
            println("public class "+className+" implements GinModule { ");
            indent();
            println("public final void configure(GinBinder binder) {");
            indent();
        }

        public String getFullName()
        {
            return fullName;
        }

        private void createIndent()
        {
            for (int i = 0; i < indent * EMPTY_PER_INDENT; i++)
            {
                w.print(" ");
            }
        }

        public void print(String line)
        {
            createIndent();
            w.write(line);
        }

        public void println(String line)
        {
            print(line);
            w.write("\n");
        }

        public void indent()
        {
            indent++;
        }

        public void dedent()
        {
            indent--;
        }

        public void close()
        {
            dedent();
            println("}");
            dedent();
            println("}");
            w.close();
        }
    }

    @Override public SourceVersion getSupportedSourceVersion()
    {
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
        if (!annotations.isEmpty())
        {
            return processGwt(annotations, roundEnv);
        }
        return true;
    }

    private boolean processGwt(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
            final Filer filer = processingEnv.getFiler();
            CharSequence relativeName = "META-INF/gwtModuleList";
            CharSequence pkg = "";
            JavaFileManager.Location location = StandardLocation.SOURCE_OUTPUT;

            File f;
            try
            {
                FileObject resource = filer.getResource(location, pkg, relativeName);
                f = new File(resource.toUri());
            }
            catch (IOException ex)
            {
                FileObject resource = filer.createResource(location, pkg, relativeName);
                f = new File(resource.toUri());
            }
            f.getParentFile().mkdirs();

            List<InjectionContext> gwtContexts = Arrays.asList(new InjectionContext[] { InjectionContext.gwt, InjectionContext.client });

            for (Element elem : roundEnv.getElementsAnnotatedWith(DefaultImplementation.class))
            {
                TypeElement typeElement = (TypeElement) elem;
                TypeElement defaultImplementationOf = getDefaultImplementationOf(typeElement);
                InjectionContext[] context = typeElement.getAnnotation(DefaultImplementation.class).context();
                List<InjectionContext> contextList = new ArrayList<InjectionContext>();

                boolean notContains = Collections.disjoint(Arrays.asList(context), gwtContexts);
                if (context.length > 0 && notContains)
                {
                    continue;
                }
                Name qualifiedName = typeElement.getQualifiedName();
                MyWriter w = new MyWriter(processingEnv, typeElement, "Impl"+defaultImplementationOf.getSimpleName());
                w.println("binder.bind(" + defaultImplementationOf + ".class).to(" + qualifiedName + ".class).in(Singleton.class);");
                w.close();
                addMetaInf(f,w.getFullName());
            }

            for (Element elem : roundEnv.getElementsAnnotatedWith(ExtensionPoint.class))
            {
                TypeElement typeElement = (TypeElement) elem;
                InjectionContext[] context = typeElement.getAnnotation(ExtensionPoint.class).context();
                List<InjectionContext> contextList = new ArrayList<InjectionContext>();

                boolean notContains = Collections.disjoint(Arrays.asList(context), gwtContexts);
                if (context.length > 0 && notContains)
                {
                    continue;
                }
                String extensionName = typeElement.getQualifiedName().toString();
                MyWriter w = new MyWriter(processingEnv, typeElement, "");
                w.println("GinMultibinder<" + extensionName + "> setBinder = GinMultibinder.newSetBinder(binder, " + extensionName + ".class);");
                w.close();
                addMetaInf(f,w.getFullName());
            }

            for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
            {
                TypeElement typeElement = (TypeElement) elem;
                TypeElement provider = getProvides(typeElement);
                Name qualifiedName = typeElement.getQualifiedName();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Provider found for " + qualifiedName);
                String extensionName = provider.getQualifiedName().toString();
                String extensionClassName = qualifiedName.toString();
                MyWriter w = new MyWriter(processingEnv, typeElement, "Extension"+provider.getSimpleName());
                w.println("GinMultibinder<" + extensionName + "> setBinder = GinMultibinder.newSetBinder(binder, " + extensionName + ".class);");
                w.println("setBinder.addBinding().to(" + extensionClassName + ".class).in(Singleton.class);");
                w.close();
                addMetaInf(f,w.getFullName());
            }
            createJavaFile(f);
        }
        catch (IOException ioe)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ioe.getMessage());
        }
        return true;
    }

    private void createJavaFile(File f) throws IOException
    {
        Filer filer = processingEnv.getFiler();
        Writer writer = null;
        try
        {
            writer = filer.createSourceFile("org.rapla.client.gwt.RaplaGwtModule").openWriter();
            writer.write("package org.rapla.client.gwt;\n");
            writer.write("import com.google.gwt.inject.client.GinModule;\n");
            writer.write("import com.google.gwt.inject.client.binder.GinBinder;\n");
            writer.write("import com.google.gwt.inject.client.multibindings.GinMultibinder;\n");
            writer.write("public class RaplaGwtModule implements GinModule{\n");
            writer.write("public void configure(GinBinder binder) {\n");
            FileObject resource;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            String line = null;
            while((line=bufferedReader.readLine())!=null)
            {
                writer.write("new "+line+"().configure(binder);\n");
            }
            writer.write("}\n");
            writer.write("}\n");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (writer!=null){
                writer.close();
            }
        }
    }

    private void generateGwtModule(){

    }

    private void addMetaInf(File f, String fullName) throws IOException
    {
        Filer filer = processingEnv.getFiler();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = null;
            while((line=reader.readLine())!=null)
            {
                if(line.equals(fullName)){
                    reader.close();
                    return;
                }
            }
            reader.close();
        }
        catch (IOException ex)
        {
        }
        PrintWriter w = new PrintWriter(new FileOutputStream(f, true));
        w.write(fullName+"\n");
        w.close();
    }

    private boolean processSwing(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        try
        {
            String packageName = "org.rapla";
            Writer writer = processingEnv.getFiler().createSourceFile(packageName + ".RaplaMainModule").openWriter();
            PrintWriter w = new PrintWriter(writer);
            w.print("package " + packageName + ";");
            w.print("public class RaplaMainModule implements org.rapla.framework.PluginDescriptor<org.rapla.client.ClientServiceContainer> { ");
            w.print("   public final void provideServices(org.rapla.client.ClientServiceContainer container, org.rapla.framework.Configuration configuration) {");

            for (Element elem : roundEnv.getElementsAnnotatedWith(Extension.class))
            {

                TypeElement typeElement = (TypeElement) elem;
                TypeElement provider = getProvides(typeElement);
                Name qualifiedName = typeElement.getQualifiedName();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Provider found for " + qualifiedName);
                //for (Class provider:provides)
                {
                    String extensionName = provider.getQualifiedName().toString();
                    String extensionClassName = qualifiedName.toString();
                    w.print("container.addContainerProvidedComponent(" + extensionName + ".class," + extensionClassName + ".class);");
                }
            }
            w.print("    }");
            w.print("}");
            w.close();
        }
        catch (IOException ioe)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ioe.getMessage());
        }
        return true;
    }

    private TypeElement getProvides(TypeElement elem)
    {
        Extension annotation = elem.getAnnotation(Extension.class);
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

    private TypeElement getExtensionPointContext(TypeElement elem)
    {
        ExtensionPoint annotation = elem.getAnnotation(ExtensionPoint.class);
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

    private TypeElement getDefaultImplementationContext(TypeElement elem)
    {
        DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
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

    private TypeElement getDefaultImplementationOf(TypeElement elem)
    {
        DefaultImplementation annotation = elem.getAnnotation(DefaultImplementation.class);
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
