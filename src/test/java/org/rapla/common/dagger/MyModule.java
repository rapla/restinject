package org.rapla.common.dagger;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import dagger.multibindings.StringKey;

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

    @Provides @IntoSet @Singleton Set<String> get1()
    {
        return Collections.singleton("Hello");
    }
    /*
    @Provides(type = Provides.Type.SET) @Singleton String get2()
    {
        return "World";
    }
    */

    @Provides @IntoMap @StringKey("1") @Singleton String getMap1()
    {
        return "Hello";
    }
    @Provides @IntoMap @StringKey("2") @Singleton String getMap2()
    {
        return "World";
    }


}
