package org.rapla.server;

import org.rapla.inject.server.RequestScoped;

@RequestScoped
public interface RemoteSession
{
    String toString();
}
