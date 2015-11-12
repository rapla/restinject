package org.rapla.common.dagger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;

@Singleton
public class MySingletonImpl implements MySingleton
{
    static int counter= 0;
    @Inject MySingletonImpl(Map<String, String> map, Set<String> test)
    {
        counter++;
        System.out.println("Singleton initialized " + map + ", " +  test);
    }
}
