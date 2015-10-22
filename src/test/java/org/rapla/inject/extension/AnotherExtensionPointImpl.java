package org.rapla.inject.extension;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.rapla.inject.Extension;

@Singleton
@Extension(id = "org.rapla.AnotherExtension", provides = ExampleExtensionPoint.class)
public class AnotherExtensionPointImpl implements ExampleExtensionPoint
{

    private static int id = 1;
    private int myId;

    @Inject
    public AnotherExtensionPointImpl()
    {
        this.myId = id++;
    }

    @Override
    public void doSomething()
    {

    }

    @Override
    public String toString()
    {
        return "AnotherExtensionPointImpl " + myId;
    }

}
