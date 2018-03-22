package org.rapla.inject.generator;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ProcessedAnnotations
{
    private Set<String> paths = new LinkedHashSet<>();
    public boolean daggerModuleRebuildNeeded;
    private Map<String, Set<String>> implementingClasses = new LinkedHashMap<>();

    public Collection<String> getPaths()
    {
        return paths;
    }

    public Collection<String> getImplementations(String interfaceName)
    {
        final Set<String> set = implementingClasses.get(interfaceName);
        if (set == null)
        {
            return Collections.emptySet();
        }
        return set;
    }

    public void addImplementation(String interfaceName, String implementingClass)
    {
        Set<String> set = implementingClasses.get(interfaceName);
        if (set == null)
        {
            set = new LinkedHashSet<>();
            implementingClasses.put(interfaceName, set);
        }
        set.add(implementingClass);
    }

    public void addPath(String path)
    {
        paths.add(path);
    }

}