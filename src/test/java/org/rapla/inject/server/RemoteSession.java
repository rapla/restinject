package org.rapla.inject.server;

import org.rapla.inject.server.RequestScoped;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@RequestScoped
public interface RemoteSession
{
    public String toString();
}
