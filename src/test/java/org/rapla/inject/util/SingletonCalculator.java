package org.rapla.inject.util;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SingletonCalculator
{

    @Inject
    public SingletonCalculator()
    {
    }

    public int add(int a, int b)
    {
        return a + b;
    }

}
