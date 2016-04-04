package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context=InjectionContext.server, of=ExampleWithParameterArray.class)
public class ExampleWithParameterArrayImpl implements ExampleWithParameterArray
{

    @Override
    public Integer[] arrayTest(String[] ids)
    {
        return new Integer[0];
    }

}
