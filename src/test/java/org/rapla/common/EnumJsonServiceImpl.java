package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.ResolvedPromise;

@DefaultImplementation(context = InjectionContext.server, of = EnumJsonService.class)
public class EnumJsonServiceImpl implements EnumJsonService
{

    @Override
    public TrueFalse insert(String comment)
    {
        return TrueFalse.TRUE;
    }

    @Override
    public TrueFalse get(Parameter param)
    {
        return TrueFalse.FALSE;
    }

    @Override
    public Promise<TrueFalse> post(Parameter param)
    {
        return new ResolvedPromise<TrueFalse>(TrueFalse.TRUE);
    }

}
