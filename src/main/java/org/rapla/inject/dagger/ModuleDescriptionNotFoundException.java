package org.rapla.inject.dagger;

import java.io.IOException;

public class ModuleDescriptionNotFoundException extends RuntimeException
{
    public ModuleDescriptionNotFoundException(String info)
    {
        super(info);
    }

    public ModuleDescriptionNotFoundException()
    {
        super("Module Description not found");
    }

    public ModuleDescriptionNotFoundException(String message, IOException ex)
    {
        super( message, ex);
    }
}
