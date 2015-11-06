package org.rapla.inject.dagger;

import dagger.Module;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;


//resolved dependency cycles currently fail with dagger, should work with dagger 2.1
@RunWith(JUnit4.class)
public class CycleTest
{
    @Singleton
    //@Component(modules = { CycleTestModule.class })
    public interface Test {
        TestInjectWithCycleA hello();
    }

    @org.junit.Test
    public void test()
    {
        TestInjectWithCycleA a = null;//Dagger_CycleTest_Test.hello();
        //final TestInjectWithCycleA a1 = a.getB().getA();
        //assertTrue(a == a1);
    }

    @Module
    public class CycleTestModule
    {
    }

    static public class TestInjectWithCycleA
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

    static public class TestInjectWithCycleB
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


}
