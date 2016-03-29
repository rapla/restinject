package org.rapla.server.rest.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.rapla.server.rest.listener.RestDaggerListener;
import org.rapla.server.StartupParams;
import org.rapla.server.dagger.DaggerRaplaServerComponent;
import org.rapla.server.dagger.DaggerRaplaServerStartupModule;
import org.rapla.server.dagger.RaplaServerComponent;

import dagger.MembersInjector;

public class InjectionFilter implements Filter
{
    private final Map<String, MembersInjector> membersInjector;

    public InjectionFilter()
    {
        try
        {
            StartupParams params = new StartupParams();
            final DaggerRaplaServerStartupModule startupModule = new DaggerRaplaServerStartupModule(params);
            final RaplaServerComponent mod = DaggerRaplaServerComponent.builder().daggerRaplaServerStartupModule(startupModule).build();
            membersInjector = mod.getComponentStarter().getMembersInjector();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        request.setAttribute(RestDaggerListener.RAPLA_CONTEXT, membersInjector);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
        // TODO Auto-generated method stub

    }
}
