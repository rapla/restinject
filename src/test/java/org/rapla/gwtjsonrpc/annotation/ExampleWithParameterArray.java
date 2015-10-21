package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.common.FutureResult;

@RemoteJsonMethod
public interface ExampleWithParameterArray
{
    FutureResult<Integer[]> arrayTest(String[] ids);
}