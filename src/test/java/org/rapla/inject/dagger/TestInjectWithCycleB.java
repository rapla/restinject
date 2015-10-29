package org.rapla.inject.dagger;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by Christopher on 28.10.2015.
 */
public class TestInjectWithCycleB
{
    final Provider<TestInjectWithCycleA> a;


    @Inject
    TestInjectWithCycleB(Provider<TestInjectWithCycleA> a)
    {
        this.a = a;
    }

    public TestInjectWithCycleA getA()
    {
        return a.get();
    }
}
