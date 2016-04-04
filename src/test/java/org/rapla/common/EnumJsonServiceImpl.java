package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

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

}
