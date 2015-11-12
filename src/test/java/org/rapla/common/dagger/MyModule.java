package org.rapla.common.dagger;

import dagger.Module;
import dagger.Provides;
import org.rapla.inject.internal.DaggerMapKey;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;

@Module public class MyModule
{
    public MyModule()
    {}
    @Provides @Singleton MySingleton provideSingleton(MySingletonImpl impl)
    {
        return impl;
    }

    @Provides(type = Provides.Type.SET_VALUES) @Singleton Set<String> get1()
    {
        return Collections.singleton("Hello");
    }
    /*
    @Provides(type = Provides.Type.SET) @Singleton String get2()
    {
        return "World";
    }
    */

    @Provides(type = Provides.Type.MAP)  @Singleton @DaggerMapKey("1") String getMap1()
    {
        return "Hello";
    }
    @Provides(type = Provides.Type.MAP) @Singleton @DaggerMapKey("2") String getMap2()
    {
        return "World";
    }


}
