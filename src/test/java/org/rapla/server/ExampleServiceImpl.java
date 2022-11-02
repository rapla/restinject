package org.rapla.server;

import org.rapla.common.ExampleService;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.ResolvedPromise;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@DefaultImplementation(context = InjectionContext.server, of = ExampleService.class)
public class ExampleServiceImpl implements ExampleService
{
    @Inject
    public ExampleServiceImpl()
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
    public Promise<Collection<Result>> sayHelloAsync(Parameter param)
    {
        final Collection<Result> results = sayHello(param);
        return new ResolvedPromise<>(results);
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
    public String helloChunk()
    {
        StringBuilder buf = new StringBuilder();
        int size = 20000;
        for (int i=0;i<size;i++)
        {
            buf.append(""+i + ";");
        }
        return buf.toString();
    }

    @Override
    public Result sayHello2(Parameter param)
    {
        Result result = new Result();
        result.setDate( param.getLastRequestTime());
        final Collection<Integer> actionIds = param.getActionIds();
        final List<String> resultIds = new ArrayList<String>();
        Map<String,Result.Moyo> map = new LinkedHashMap<>();
        final Result.Moyo value = new Result.Moyo();
        value.setAsd("Test");
        map.put("1", value);
        result.setMap( map);
        for (Integer id : actionIds)
        {
            resultIds.add(id.toString());
        }
        result.setIds(resultIds);
        return result;
    }

    @Override
    public Promise<Map<String, Set<String>>> complex(Map<String,String> param)
    {

        Map<String, Set<String>> list = new LinkedHashMap<>();
        Set<String> set = new LinkedHashSet<String>();
        set.add("Hello");
        set.add(param.get("greeting"));
        list.put("greeting", set);
        return new ResolvedPromise<>(list);
    }

    @Override public List<Result> sayHello6()
    {
        final Parameter param = new Parameter();
        param.setActionIds(Arrays.asList(new Integer[]{1}));
        List<Result> list = sayHello3(param);
        return list;
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Override public String collections(@QueryParam("param") Collection<String> test)
    {
        return "Made" + test.toString();
    }

    @Override public String list(@QueryParam("param") List<String> test)
    {
        return "Made" + test.toString();
    }
    @Override public String set(@QueryParam("param") Set<String> test)
    {
        return "Made" + test.toString();
    }


//    @Override
//    public String charArray(Character[] charArray1,char[] charArray2)
//    {
//        return "Made"  + Arrays.toString(charArray1) +"," + Arrays.toString( charArray2);
//    }

        public String charArray(char[] charArray2)
        {
            return "Made"  + Arrays.toString( charArray2);
        }


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("listWithBody")
    @Override
    public String listWithBody(@QueryParam("param") String test, List<Parameter> postBody)
    {
        return "Made" +postBody.toString();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("setWithBody")
    @Override
    public String setWithBody(Set<Parameter> postBody)
    {
        return "Made" +postBody.toString();
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
