package org.rapla.inject;


@Extension(provides = InjectionTest.class, id=InjectionTestImpl.ID)
public class InjectionTestImpl implements  InjectionTest
{
    public static final String ID = "org.rapla.testimpl";
}
