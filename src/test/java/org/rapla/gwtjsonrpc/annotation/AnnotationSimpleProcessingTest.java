package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.common.FutureResult;

@RemoteJsonMethod
public interface AnnotationSimpleProcessingTest
{
    FutureResult<String> sayHello(String param);
}