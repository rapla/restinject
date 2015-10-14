package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.ResultType;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RemoteJsonMethod
public interface AnnotationProcessingTest
{
    @ResultType(Result.class)
    FutureResult<Result> sayHello(Parameter param);
    
    public static class Result {
        private String name;
        private List<String> ids;
        
    }
    
    public static class Parameter
    {
        //private Map<String, List<String>> requestedIds;
        private List<Integer> actionIds;
        private Date lastRequestTime;
    }
}