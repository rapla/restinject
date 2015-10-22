package org.rapla.inject.util;

import javax.inject.Inject;

public class Calculator
{
    @Inject
    public Calculator()
    {
    }

    public int add(int a, int b)
    {
        return a + b;
    }
}
