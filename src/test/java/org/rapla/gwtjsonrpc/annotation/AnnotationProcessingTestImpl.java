package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.ResultImpl;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.server.RemoteSession;
import org.rapla.inject.server.RequestScoped;
import org.rapla.server.TestServer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;



@RequestScoped
@DefaultImplementation(of=AnnotationProcessingTest.class,context = InjectionContext.server)
public class AnnotationProcessingTestImpl implements AnnotationProcessingTest
{
    @Inject
    public AnnotationProcessingTestImpl()
    {
    }

    @Override public FutureResult<List<Result>> sayHello(Parameter param)
    {
        List<Result> list = sayHello3(param);
        final ResultImpl<List<Result>> futureResult = new ResultImpl<>(list);
        return futureResult;
    }

    @Override public List<Result> sayHello3(Parameter param)
    {
        Result result = sayHello2(param);
        List<Result> list = new ArrayList<>();
        list.add( result );
        return list;
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
