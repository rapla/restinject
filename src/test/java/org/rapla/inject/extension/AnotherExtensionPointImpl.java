package org.rapla.inject.extension;

import javax.inject.Inject;

import org.rapla.inject.Extension;

@Extension(id = "org.rapla.AnotherExtension", provides = ExampleExtensionPoint.class)
public class AnotherExtensionPointImpl implements ExampleExtensionPoint
{
    @Inject
    public AnotherExtensionPointImpl()
    {
    }

    @Override
    public void doSomething()
    {

    }

}
