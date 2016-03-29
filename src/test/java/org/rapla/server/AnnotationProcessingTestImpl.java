package org.rapla.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.rapla.common.AnnotationProcessingTest;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.jsonrpc.common.FutureResult;
import org.rapla.jsonrpc.common.ResultImpl;

@DefaultImplementation(context = InjectionContext.server, of = AnnotationProcessingTest.class)
public class AnnotationProcessingTestImpl implements AnnotationProcessingTest
{
    @Inject
    public AnnotationProcessingTestImpl()
    {
    }

    @Override
    public FutureResult<List<Result>> sayHello(Parameter param)
    {
        List<Result> list = sayHello3(param);
        final ResultImpl<List<Result>> futureResult = new ResultImpl<>(list);
        return futureResult;
    }

    @Override
    public List<Result> sayHello3(Parameter param)
    {
        Result result = sayHello2(param);
        List<Result> list = new ArrayList<>();
        list.add(result);
        return list;
    }

    @Override
    public Result sayHello2(Parameter param)
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

    @Override
    public FutureResult<Map<String, Set<String>>> complex()
    {

        Map<String, Set<String>> list = new LinkedHashMap<>();
        Set<String> set = new LinkedHashSet<String>();
        set.add("Hello");
        set.add("World");
        list.put("greeting", set);
        final ResultImpl<Map<String, Set<String>>> futureResult = new ResultImpl<>(list);
        return futureResult;
    }
}
