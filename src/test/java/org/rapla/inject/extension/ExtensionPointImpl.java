package org.rapla.inject.extension;

import javax.inject.Inject;

import org.rapla.inject.Extension;
import org.rapla.inject.util.Calculator;
import org.rapla.inject.util.SingletonCalculator;

@Extension(id = "org.rapla.ExtensionPointImpl", provides = ExampleExtensionPoint.class)
public class ExtensionPointImpl implements ExampleExtensionPoint
{

    private Calculator calc;
    private SingletonCalculator singCalc;

    @Inject
    public ExtensionPointImpl(Calculator calc, SingletonCalculator singCalc)
    {
        this.calc = calc;
        this.singCalc = singCalc;
    }

    @Override
    public void doSomething()
    {

    }

    @Override
    public String toString()
    {
        return "calc: " + calc.toString() + "\nsingCalc: " + singCalc.toString();
    }

}
