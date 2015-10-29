package org.rapla.inject.dagger;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by Christopher on 28.10.2015.
 */
public class TestInjectWithCycleA
{
    Provider<TestInjectWithCycleB> b;
    @Inject TestInjectWithCycleA(Provider<TestInjectWithCycleB> b)
    {
        this.b = b;
    }

    public TestInjectWithCycleB getB()
    {
        return b.get();
    }
}
