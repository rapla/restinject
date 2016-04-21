package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import javax.ws.rs.QueryParam;
import java.util.Arrays;
import java.util.List;

@DefaultImplementation(context=InjectionContext.server, of=ExampleWithParameterArray.class)
public class ExampleWithParameterArrayImpl implements ExampleWithParameterArray
{

    @Override
    public Integer[] arrayTest(String[] ids)
    {
        return new Integer[0];
    }

    @Override public List<Integer> arrayTest(@QueryParam("ids") List<String> ids)
    {
        return Arrays.asList(arrayTest( ids.toArray(new String[] {})));
    }

}
