package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.jsonrpc.client.gwt.internal.impl.FutureResultImpl;
import org.rapla.jsonrpc.common.FutureResult;

@DefaultImplementation(context=InjectionContext.server, of=ExampleWithParameterArray.class)
public class ExampleWithParameterArrayImpl implements ExampleWithParameterArray
{

    @Override
    public FutureResult<Integer[]> arrayTest(String[] ids)
    {
        return new FutureResultImpl<>();
    }

}
