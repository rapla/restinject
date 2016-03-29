package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.jsonrpc.client.gwt.internal.impl.FutureResultImpl;
import org.rapla.jsonrpc.common.FutureResult;

@DefaultImplementation(context = InjectionContext.server, of = EnumJsonService.class)
public class EnumJsonServiceImpl implements EnumJsonService
{

    @Override
    public FutureResult<TrueFalse> insert(String comment)
    {
        return new FutureResultImpl<>();
    }

    @Override
    public TrueFalse get(Parameter param)
    {
        return TrueFalse.FALSE;
    }

}
