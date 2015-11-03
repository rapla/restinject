package org.rapla.inject.server;

import dagger.Module;
import dagger.Provides;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Module
public class RequestModule
{
    HttpServletRequest request;
    HttpServletResponse response;
    public RequestModule(HttpServletRequest request, HttpServletResponse response)
    {
        this.request = request;this.response = response;
    }

    @Provides
    public HttpServletRequest provideRequest()
    {
        return request;
    }

    @Provides
    public HttpServletResponse provideResponse()
    {
        return response;
    }
}
