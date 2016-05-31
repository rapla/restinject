package org.rapla.inject.scanning;

public class RestEasyLoadingFilter implements ServiceInfLoader.LoadingFilter
{
    String[] ignoredPackages = { "org.jboss.resteasy.plugins", "org.jboss.resteasy.annotations", "org.jboss.resteasy.client", "org.jboss.resteasy.specimpl",
            "org.jboss.resteasy.core", "org.jboss.resteasy.spi", "org.jboss.resteasy.util", "org.jboss.resteasy.mock", "javax.ws.rs" };

    @Override public boolean classNameShouldBeIgnored(String classname)
    {
        return (classname.endsWith("JavaJsonProxy") || classname.endsWith("GwtJsonProxy"));
    }

    @Override public String[] getIgnoredPackages()
    {
        return ignoredPackages;
    }
}
