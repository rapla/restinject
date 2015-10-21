package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;

@RemoteJsonMethod
public interface SimpleService
{
    void sayHello();
    
    boolean isDone();
    
    int getStepCount();

    double getPercentage();
}
