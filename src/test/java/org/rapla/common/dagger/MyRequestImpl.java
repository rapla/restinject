package org.rapla.common.dagger;

import org.rapla.server.RequestScoped;

import javax.inject.Inject;
import java.util.Set;

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
