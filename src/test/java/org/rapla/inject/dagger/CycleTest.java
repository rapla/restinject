package org.rapla.inject.dagger;

import dagger.Component;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Singleton;



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


}
