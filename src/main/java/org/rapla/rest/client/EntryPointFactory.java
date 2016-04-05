package org.rapla.rest.client;

public interface EntryPointFactory {
    public String getEntryPoint(String interfaceName, String relativePath);
}
