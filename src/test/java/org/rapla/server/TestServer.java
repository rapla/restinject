package org.rapla.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface TestServer
{
    void service(HttpServletRequest request, HttpServletResponse response) throws Exception;
}