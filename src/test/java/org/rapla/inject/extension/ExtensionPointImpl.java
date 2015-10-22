package org.rapla.inject.extension;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.rapla.inject.Extension;

@Singleton
@Extension(id = "org.rapla.ExtensionPointImpl", provides = ExampleExtensionPoint.class)
public class ExtensionPointImpl implements ExampleExtensionPoint
{

    @Inject
    public ExtensionPointImpl()
    {
    }

    @Override
    public void doSomething()
    {

    }

}
