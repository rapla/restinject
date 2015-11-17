package org.rapla.common.dagger;

import dagger.Component;
import dagger.Subcomponent;
import org.rapla.inject.server.RequestScoped;

import javax.inject.Singleton;

@Singleton @Component(modules = {MyModule.class}) public interface MyComponent
{
    MyServer getServer();

    @RequestScoped @Subcomponent(modules = MyRequestModule.class) interface MySubComponent
    {
        MyRequest getRequest();
    }

    MySubComponent getRequestComponent(MyRequestModule requestModule);
    //

}
