package org.rapla.common.dagger;

import dagger.Module;
import dagger.Provides;
import org.rapla.inject.server.RequestScoped;

/**
 * Created by Christopher on 09.11.2015.
 */
@Module public class MyRequestModule
{
    MyHttpRequest request;

    public MyRequestModule(MyHttpRequest request)
    {
        this.request = request;
    }

    @Provides MyHttpRequest getHttpRequest()
    {
        return request;
    }

    @Provides @RequestScoped MyRequest provideRequest(MyRequestImpl impl)
    {
        return impl;
    }


}
