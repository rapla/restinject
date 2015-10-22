package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.ResultImpl;

import java.util.ArrayList;
import java.util.List;

public class AnnotationProcessingTestImpl implements AnnotationProcessingTest
{
    @Override public FutureResult<Result> sayHello(Parameter param)
    {
        Result result = sayHello2(param);
        final ResultImpl<Result> futureResult = new ResultImpl<Result>(result);
        return futureResult;
    }

    @Override public Result sayHello2(Parameter param)
    {
        Result result = new Result();
        final List<Integer> actionIds = param.getActionIds();
        final List<String> resultIds = new ArrayList<String>();
        for (Integer id : actionIds)
        {
            resultIds.add(id.toString());
        }
        result.setIds(resultIds);
        return result;
    }
}
