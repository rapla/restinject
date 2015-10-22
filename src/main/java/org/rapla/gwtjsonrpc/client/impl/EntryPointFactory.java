package org.rapla.gwtjsonrpc.client.impl;

public interface EntryPointFactory {
    public String getEntryPoint(String interfaceName, String relativePath);
}
