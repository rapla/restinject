package org.rapla.common;

import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.common.FutureResult;

@RemoteJsonMethod
public interface AnnotationSimpleProcessingTest
{
    FutureResult<String> sayHello(String param);
}