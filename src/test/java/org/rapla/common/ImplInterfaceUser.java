package org.rapla.common;

import javax.inject.Inject;

public class ImplInterfaceUser
{
    private ImplInterface oi;

    @Inject
    public ImplInterfaceUser(ImplInterface oi)
    {
        this.oi = oi;
    }
    
    public boolean isImplInterfaceClass(Class<? extends ImplInterface> oiClass)
    {
        return oi.getClass().equals(oiClass);
    }
}
