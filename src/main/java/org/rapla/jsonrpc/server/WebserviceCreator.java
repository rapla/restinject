package org.rapla.jsonrpc.server;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface WebserviceCreator
{
    Object create(HttpServletRequest request,HttpServletResponse response);
}
