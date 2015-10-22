package org.rapla.inject;

import javax.inject.Inject;

@Extension(provides = InjectionTest.class, id = InjectionTestImpl.ID)
public class InjectionTestImpl implements InjectionTest
{
    public static final String ID = "org.rapla.testimpl";

    @Inject
    public InjectionTestImpl()
    {
    }

}
