package org.rapla.jsonrpc.client;

public interface EntryPointFactory {
    public String getEntryPoint(String interfaceName, String relativePath);
}
