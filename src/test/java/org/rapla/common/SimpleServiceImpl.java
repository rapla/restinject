package org.rapla.common;

import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context = InjectionContext.server, of = SimpleService.class)
public class SimpleServiceImpl implements SimpleService
{

    @Override
    public void sayHello()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isDone()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getStepCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getPercentage()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
