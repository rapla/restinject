package org.rapla.common.extension;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.rapla.inject.Extension;
import org.rapla.common.util.Calculator;
import org.rapla.common.util.SingletonCalculator;

@Singleton
@Extension(id = "org.rapla.AnotherExtension", provides = ExampleExtensionPoint.class)
public class AnotherExtensionPointImpl implements ExampleExtensionPoint
{

    private static int id = 1;
    private int myId;
    private Calculator calc;
    private SingletonCalculator singleCalc;

    @Inject
    public AnotherExtensionPointImpl(Calculator calc, SingletonCalculator singleCalc)
    {
        this.calc = calc;
        this.singleCalc = singleCalc;
        this.myId = id++;
    }

    @Override
    public void doSomething()
    {

    }

    @Override
    public String toString()
    {
        return "AnotherExtensionPointImpl " + myId + "\ncalc: " + calc.toString() + "\nsinglCalc: " + singleCalc.toString();
    }

}
