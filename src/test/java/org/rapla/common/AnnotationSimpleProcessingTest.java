package org.rapla.common;

import java.util.List;

import org.rapla.jsonrpc.common.FutureResult;
import org.rapla.jsonrpc.common.RemoteJsonMethod;

@RemoteJsonMethod
public interface AnnotationSimpleProcessingTest
{
    FutureResult<String> sayHello(String param);
    
    List<String> translations(String id);
}