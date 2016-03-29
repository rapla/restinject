package org.rapla.server;

import javax.servlet.http.HttpServletRequest;

public interface RemoteSession
{
    String toString(HttpServletRequest request);
}
