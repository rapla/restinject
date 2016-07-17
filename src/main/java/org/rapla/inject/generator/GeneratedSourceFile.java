package org.rapla.inject.generator;

public class GeneratedSourceFile
{
    private final String interfaceName;
    private final String className;
    private final String id;

    public GeneratedSourceFile(String interfaceName, String className, String id)
    {
        super();
        this.interfaceName = interfaceName;
        this.className = className;
        this.id = id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((interfaceName == null) ? 0 : interfaceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GeneratedSourceFile other = (GeneratedSourceFile) obj;
        if (className == null)
        {
            if (other.className != null)
                return false;
        }
        else if (!className.equals(other.className))
            return false;
        if (id == null)
        {
            if (other.id != null)
                return false;
        }
        else if (!id.equals(other.id))
            return false;
        if (interfaceName == null)
        {
            if (other.interfaceName != null)
                return false;
        }
        else if (!interfaceName.equals(other.interfaceName))
            return false;
        return true;
    }

}