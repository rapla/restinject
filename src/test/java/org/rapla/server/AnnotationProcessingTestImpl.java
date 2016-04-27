package org.rapla.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.QueryParam;

import org.rapla.common.AnnotationProcessingTest;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context = InjectionContext.server, of = AnnotationProcessingTest.class)
public class AnnotationProcessingTestImpl implements AnnotationProcessingTest
{
    @Inject
    public AnnotationProcessingTestImpl()
    {
    }

    @Override
    public void dontSayHello()
    {

    }

    @Override
    public Collection<Result> sayHello(Parameter param)
    {
        List<Result> list = sayHello3(param);
        return list;
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
        final Collection<Integer> actionIds = param.getActionIds();
        final List<String> resultIds = new ArrayList<String>();
        for (Integer id : actionIds)
        {
            resultIds.add(id.toString());
        }
        result.setIds(resultIds);
        return result;
    }

    @Override
    public Map<String, Set<String>> complex(Map<String,String> param)
    {

        Map<String, Set<String>> list = new LinkedHashMap<>();
        Set<String> set = new LinkedHashSet<String>();
        set.add("Hello");
        set.add(param.get("greeting"));
        list.put("greeting", set);
        return list;
    }

    @Override public List<Result> sayHello6()
    {
        final Parameter param = new Parameter();
        param.setActionIds(Arrays.asList(new Integer[]{1}));
        List<Result> list = sayHello3(param);
        return list;
    }

    @Override public String collecions(@QueryParam("param") Collection<String> test, @QueryParam("complex") Collection<Parameter> complex)
    {
        return "Made" + test.toString()+","+complex.toString();
    }

    @Override public String longcall()
    {
        try
        {
            Thread.sleep( 1000);
        }
        catch (InterruptedException e)
        {
        }
        return "long";
    }

    @Override public String shortcall()
    {
        try
        {
            Thread.sleep( 10);
        }
        catch (InterruptedException e)
        {
        }
        return "short";
    }
}
