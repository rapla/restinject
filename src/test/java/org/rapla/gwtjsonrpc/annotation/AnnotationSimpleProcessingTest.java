package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.ResultType;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RemoteJsonMethod
public interface AnnotationSimpleProcessingTest
{
    FutureResult<String> sayHello(String param);
}