package org.rapla.common;

import org.rapla.jsonrpc.common.RemoteJsonMethod;

@RemoteJsonMethod
public interface SimpleService
{
    void sayHello();
    
    boolean isDone();
    
    int getStepCount();

    double getPercentage();
}
