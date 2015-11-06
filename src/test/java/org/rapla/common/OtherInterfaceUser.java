package org.rapla.common;

import javax.inject.Inject;

public class OtherInterfaceUser
{
    private OtherInterface oi;

    @Inject
    public OtherInterfaceUser(OtherInterface oi)
    {
        this.oi = oi;
    }
    
    public boolean isOtherInterfaceClass(Class<? extends OtherInterface> oiClass)
    {
        return oi.getClass().equals(oiClass);
    }
}
