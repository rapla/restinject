package org.rapla.common.extension;

import org.rapla.inject.Extension;

import javax.inject.Inject;

@Extension(provides = InjectionTestInterface.class, id = InjectionTestImpl.ID)
public class InjectionTestImpl implements InjectionTestInterface
{
    public static final String ID = "org.rapla.testimpl";

    @Inject
    public InjectionTestImpl()
    {
    }

}
