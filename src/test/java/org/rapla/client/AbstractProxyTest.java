package org.rapla.client;

import org.junit.Before;
import org.junit.Test;
import org.rapla.common.ExampleService;
import org.rapla.common.ExampleSimpleService;
import org.rapla.logger.ConsoleLogger;
import org.rapla.logger.Logger;
import org.rapla.rest.client.CustomConnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractProxyTest
{
    protected Map<String, String> paramMap;
    protected CustomConnector connector;
    protected Logger logger = new ConsoleLogger();

    protected abstract CustomConnector createConnector();

    @Before public void setUp() throws Exception
    {
        paramMap = new LinkedHashMap<>();
        paramMap.put("greeting", "World");
        connector = createConnector();
    }

    protected abstract ExampleService createExampleServiceProxy();

    protected abstract ExampleSimpleService createExampleSimpleServiceProxy();
/**/
    @Test public void test() throws Exception
    {
        ExampleService test = createExampleServiceProxy();
        test.dontSayHello();

        ExampleService.Parameter p = new ExampleService.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        Date date = new Date( System.currentTimeMillis());
        p.setLastRequestTime( date);
        final Collection<ExampleService.Result> resultList = test.sayHello(p);
        final ExampleService.Result result = resultList.iterator().next();
        final Collection<String> ids = result.getIds();
        assertEq(2, ids.size());
        final Iterator<String> iterator = ids.iterator();
        assertEq("1", iterator.next());
        assertEq("2", iterator.next());
        final ExampleService.Result result1 = test.sayHello2(p);
        assertEq( date, result1.getDate());
        final Map<String, ExampleService.Result.Moyo> map = result1.getMap();
        if ( map == null)
        {
            fail_("Map is null");
        }
        final ExampleService.Result.Moyo moyo = map.get("1");
        final String asd = moyo.getAsd();
        assertEq("Test", asd);
        test.sayHello3(p);

    }

    abstract public void assertEq(Object o1, Object o2);

    abstract public void fail_(String message);

    @Test public void test3() throws Exception
    {
        ExampleSimpleService test = createExampleSimpleServiceProxy();

        {
            final String result = test.postHello(null);
            assertEq(Boolean.TRUE, (Boolean) result.startsWith("null"));
        }
        {
            final String result = test.postHello("");
            assertEq(Boolean.TRUE, (Boolean) !result.startsWith("null"));
        }
        final String message = "hello";
        {
            final String result = test.sayHello(message);
            assertEq(Boolean.TRUE, (Boolean) result.startsWith(message));
        }
    }

    @Test public void testListOfStrings() throws Exception
    {
        ExampleSimpleService test = createExampleSimpleServiceProxy();
        final String message = "hello";
        final List<String> resultFutureResult = test.translations(message);
        assertEq(3, resultFutureResult.size());
        assertEq(message, resultFutureResult.get(0));
        assertEq(message + "_de", resultFutureResult.get(1));
        assertEq(message + "_fr", resultFutureResult.get(2));
    }

    @Test
    public void testDate() throws Exception
    {
        Date currentDate = new Date(System.currentTimeMillis());
        final ExampleSimpleService exampleSimpleServiceProxy = createExampleSimpleServiceProxy();
        final Date nextDay = exampleSimpleServiceProxy.addDay(currentDate);
        assertEq(currentDate.getTime() + 1000l * 60l * 60l * 24l, nextDay.getTime());
    }

    @Test public void testException() throws Exception
    {
        ExampleSimpleService test = createExampleSimpleServiceProxy();
        try
        {
            final List<String> exception = test.exception();
            fail_("Exception should have been thrown");
        }
        catch (RuntimeException e)
        {
            final String message = e.getMessage();
            assertEq("Something went wrong", message);
        }
    }

    @Test public void testCollections() throws Exception
    {
        ExampleService test = createExampleServiceProxy();
        Collection<String> primiteCollection = Arrays.asList(new String[] { "Hello", "World" });
        Collection<ExampleService.Parameter> complectCollection = new LinkedHashSet<>();
        complectCollection.add(new ExampleService.Parameter());
        final String greeting = test.collections(primiteCollection);
        assertEq(
                "Made[Hello, World]",
                greeting);
    }


    @Test
    public void testChunk() throws Exception
    {
        ExampleService test = createExampleServiceProxy();
        final String s = test.helloChunk();
        final String[] split = s.split(";");
        assertEq("0", split[0]);
        assertEq("" + (split.length-1), split[split.length-1]);
    }

    @Test
    public void testLists() throws Exception
    {
        ExampleService test = createExampleServiceProxy();
        List<String> primiteCollection = Arrays.asList(new String[] { "Hello", "World" });
        List<ExampleService.Parameter> complectCollection = new LinkedList<>();
        final ExampleService.Parameter param2 = new ExampleService.Parameter();
        param2.setCasts(primiteCollection);
        complectCollection.add(param2);
        List<ExampleService.Parameter> body = new ArrayList<>(complectCollection);
        final String greeting = test.listWithBody("Hello", body);
        assertEq(
                "Made[{casts=[Hello, World]}]",
                greeting);
    }


    @Test
    public void testSets() throws Exception
    {
        ExampleService test = createExampleServiceProxy();
        Set<String> primiteCollection = new LinkedHashSet(Arrays.asList(new String[] { "Hello", "World" }));
        Set<ExampleService.Parameter> complectCollection = new LinkedHashSet<>();
        final ExampleService.Parameter param2 = new ExampleService.Parameter();
        param2.setCasts(primiteCollection);
        complectCollection.add(param2);
        Set<ExampleService.Parameter> body = new LinkedHashSet<>(complectCollection);
        final String greeting = test.setWithBody( body);
        assertEq(
                "Made[{casts=[Hello, World]}]",
                greeting);
    }



    @Test public void testEncoding() throws Exception
    {
        String stringWithDifficultCharacters = "#äöüßÖÄÜ\"+<>!§$%&(=&\\)";
        final String result = createExampleSimpleServiceProxy().sayHello(stringWithDifficultCharacters);
        final String substring = result.substring(0, stringWithDifficultCharacters.length());
        assertEq(stringWithDifficultCharacters, substring);
    }

    @Test public void testPrimitiveString() throws Exception
    {
        final ExampleSimpleService exampleSimpleServiceProxy = createExampleSimpleServiceProxy();
        String param = "another param";
        final String result = exampleSimpleServiceProxy.sendString(param);
        assertEq(param, result);
    }

    @Test public void testDouble() throws Exception
    {
        final ExampleSimpleService exampleSimpleServiceProxy = createExampleSimpleServiceProxy();
        Double param = new Double(-2.0);
        final Double result = exampleSimpleServiceProxy.sendDouble(param);
        assertEq(param, -1 * result);
    }

    @Test public void testPrimDouble() throws Exception
    {
        final ExampleSimpleService exampleSimpleServiceProxy = createExampleSimpleServiceProxy();
        double param = 12.;
        final double result = exampleSimpleServiceProxy.sendPrimDouble(param);
        assertEq(param, -1 * result);
    }

    @Test public void testBoolean() throws Exception
    {
        Boolean param = Boolean.FALSE;
        final Boolean result = createExampleSimpleServiceProxy().sendBool(param);
        assertEq(param, !result);
    }

    @Test public void testPrimBoolean() throws Exception
    {
        boolean param = true;
        final boolean result = createExampleSimpleServiceProxy().sendPrimBool(param);
        assertEq(param, !result);
    }

    @Test public void testInt() throws Exception
    {
        final ExampleSimpleService exampleSimpleServiceProxy = createExampleSimpleServiceProxy();
        {
            Integer param = new Integer(42);
            {
                final Integer result = exampleSimpleServiceProxy.sendInt(param);
                assertEq(param, -1 * result);
            }
            {
                final Integer result = exampleSimpleServiceProxy.postInt(param);
                assertEq(param, -1 * result);
            }
        }
        {
            final Integer result = exampleSimpleServiceProxy.postInt(null);
            assertEq(null, result);
        }
    }

    @Test public void testPrimInt() throws Exception
    {
        int param = 24;
        final int result = createExampleSimpleServiceProxy().sendPrimInt(param);
        assertEq(param, -1 * result);
    }

    @Test public void testChar() throws Exception
    {
        Character param = new Character('a');
        final Character result = createExampleSimpleServiceProxy().sendChar(param);
        assertEq(new Character('b'), result);
    }
}
