package org.rapla.inject.generator;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

import org.rapla.inject.generator.AnnotationInjectionProcessor.Scopes;

public class ModuleInfo
{
    final public String groupId;
    final public String artifactId;
    final Set<ModuleInfo> parentModules = new HashSet<ModuleInfo>();

    public ModuleInfo(ProcessingEnvironment processingEnv) 
    {
        this(getModuleName(processingEnv));
        final String[] parents = getParentModuleNames(processingEnv);
        for (String parent:parents)
        {
            ModuleInfo parentModule = new ModuleInfo( parent );
            parentModules.add(parentModule);
        }
    }
    
    private ModuleInfo(String moduleName) 
    {
        final int i = moduleName.lastIndexOf(".");
        groupId = (i >= 0 ? moduleName.substring(0, i) : "");
        artifactId = AnnotationInjectionProcessor.firstCharUp(i >= 0 ? moduleName.substring(i + 1) : moduleName);
    }

    static public String getModuleName(ProcessingEnvironment processingEnv)
    {
        String moduleName = null;
        moduleName = processingEnv.getOptions().get(AnnotationInjectionProcessor.MODULE_NAME_OPTION);
        if (moduleName != null)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found param moduleName: " + moduleName);
            return moduleName;
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "moduleName not found using " + moduleName);
        return moduleName;
    }
    
      
    
    
    private String[] getParentModuleNames(ProcessingEnvironment processingEnv)
    {
        String modules = processingEnv.getOptions().get(AnnotationInjectionProcessor.PARENT_MODULES_OPTION);
        if ( modules != null)
        {
            return modules.split(",");
        }
        return new String[] {};
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }
    
    public Set<ModuleInfo> getParentModules()
    {
        return parentModules;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        final String string = getModuleName();
        builder.append( string);
        if ( parentModules.size() >= 0)
        {
            builder.append("<--");
            boolean first = true;
            for (ModuleInfo parent:parentModules)
            {
                if ( first)
                {
                    first = false;
                }
                else
                {
                    builder.append(", ");
                }
                builder.append(parent.getModuleName());
            }
        }
        return builder.toString();
    }

    private String getModuleName()
    {
        final String string = groupId + "." + artifactId;
        return string;
    }
    
    public String getScopedModuleName(Scopes scope)
    {
        String moduleName = scope.getPackageName(getGroupId())+ ".Dagger"+getArtifactId() + scope.toString() + "Module";
        return moduleName;
        
    }
    public String getScopedComponentName(Scopes scope)
    {
        String scopedPackageName = scope.getPackageName(getGroupId());
        final String componentName = scopedPackageName + "." + getArtifactId() + scope.toString() + "Component";
        return componentName;
    }

    public String getFullModuleName(Scopes scope)
    {
        return scope.getPackageName(groupId) + "." + getSimpleModuleName( scope);
    }
    
    public String getFullStartupModuleName(Scopes scope)
    {
        return scope.getPackageName(groupId) + "." + "Dagger" + artifactId + scope+ "StartupModule";
    }

    public String getSimpleModuleName( Scopes scope)
    {
        return "Dagger" + artifactId + scope + "Module";
    }

    public String getSimpleComponentName( Scopes scope)
    {
        return artifactId + scope.toString() + "Component";
    }
}