package org.rapla.gwtjsonrpc.annotation;
import org.rapla.gwtjsonrpc.RemoteJsonMethod;

@RemoteJsonMethod
public interface AnnotationProcessingTest
{
    String sayHello(String message);
}
