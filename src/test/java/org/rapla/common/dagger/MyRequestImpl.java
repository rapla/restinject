package org.rapla.common.dagger;

import org.rapla.inject.server.RequestScoped;

import javax.inject.Inject;

@RequestScoped
public class MyRequestImpl implements MyRequest
{
    static int counter= 0;
    @Inject
    public MyRequestImpl(MySingleton singleton, MyHttpRequest request)
    {
        counter++;
        System.out.println("Request initialized " );
    }


}
