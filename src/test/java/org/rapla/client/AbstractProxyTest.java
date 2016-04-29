package org.rapla.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest.Parameter;
import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.rest.client.CustomConnector;

public abstract class AbstractProxyTest
{
    protected Map<String, String> paramMap;
    protected CustomConnector connector;

    protected abstract CustomConnector createConnector();

    @Before
    public void setUp() throws Exception
    {
        paramMap = new LinkedHashMap<>();
        paramMap.put("greeting", "World");
        connector = createConnector();
    }

    protected abstract AnnotationProcessingTest createAnnotationProcessingProxy();

    protected abstract AnnotationSimpleProcessingTest createAnnotationSimpleProxy();

    @Test
    public void test() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        test.dontSayHello();

        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final Collection<AnnotationProcessingTest.Result> resultList = test.sayHello(p);
        final AnnotationProcessingTest.Result result = resultList.iterator().next();
        final Collection<String> ids = result.getIds();
        assertEq(2, ids.size());
        final Iterator<String> iterator = ids.iterator();
        assertEq("1", iterator.next());
        assertEq("2", iterator.next());
        test.sayHello2(p);
        test.sayHello3(p);

    }

    abstract public void assertEq(Object o1, Object o2);

    abstract public void fail_(String message);

    @Test
    public void test3() throws Exception
    {
        AnnotationSimpleProcessingTest test = createAnnotationSimpleProxy();
        final String message = "hello";
        final String result = test.sayHello(message);
        System.out.println("result");
        assertEq(Boolean.TRUE, (Boolean) result.startsWith(message));
    }

    @Test
    public void testListOfStrings() throws Exception
    {
        AnnotationSimpleProcessingTest test = createAnnotationSimpleProxy();
        final String message = "hello";
        final List<String> resultFutureResult = test.translations(message);
        assertEq(3, resultFutureResult.size());
        assertEq(message, resultFutureResult.get(0));
        assertEq(message + "_de", resultFutureResult.get(1));
        assertEq(message + "_fr", resultFutureResult.get(2));
    }

    @Test
    public void test4() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        final Set<String> greeting = test.complex(paramMap).get("greeting");
        assertEq(2, greeting.size());
        final Iterator<String> iterator = greeting.iterator();
        assertEq("Hello", iterator.next());
        assertEq("World", iterator.next());
    }

    @Test
    public void testException() throws Exception
    {
        AnnotationSimpleProcessingTest test = createAnnotationSimpleProxy();
        try
        {
            final List<String> exception = test.exception();
            fail_("Exception should have been thrown");
        }
        catch (RuntimeException e)
        {

        }
    }

    @Test
    public void testCollections() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        Collection<String> primiteCollection = Arrays.asList(new String[] { "Hello", "World" });
        Collection<AnnotationProcessingTest.Parameter> complectCollection = new LinkedHashSet<>();
        complectCollection.add(new AnnotationProcessingTest.Parameter());
        final AnnotationProcessingTest.Parameter param2 = new AnnotationProcessingTest.Parameter();
        param2.setCasts(primiteCollection);
        complectCollection.add(param2);
        final String greeting = test.collecions(primiteCollection, complectCollection);
        assertEq(
                "Made[Hello, World],[Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=null}, Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=[Hello, World]}]",
                greeting);
    }
    
    
    @Test
    public void testLists() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        List<String> primiteCollection = Arrays.asList(new String[] { "Hello", "World" });
        List<AnnotationProcessingTest.Parameter> complectCollection = new LinkedList<>();
        complectCollection.add(new AnnotationProcessingTest.Parameter());
        final AnnotationProcessingTest.Parameter param2 = new AnnotationProcessingTest.Parameter();
        param2.setCasts(primiteCollection);
        complectCollection.add(param2);
        List<Parameter> body = new ArrayList<>(complectCollection);
        final String greeting = test.list(primiteCollection, complectCollection, body);
        assertEq(
                "Made[\"Hello\", \"World\"],[Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=null}, Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=[Hello, World]}],[{}, {casts=[Hello, World]}]",
                greeting);
    }
    
    
    @Test
    public void testSets() throws Exception
    {
        AnnotationProcessingTest test = createAnnotationProcessingProxy();
        Set<String> primiteCollection = new LinkedHashSet(Arrays.asList(new String[] { "Hello", "World" }));
        Set<AnnotationProcessingTest.Parameter> complectCollection = new LinkedHashSet<>();
        final AnnotationProcessingTest.Parameter param2 = new AnnotationProcessingTest.Parameter();
        param2.setCasts(primiteCollection);
        complectCollection.add(param2);
        Set<Parameter> body = new LinkedHashSet<>(complectCollection);
        final String greeting = test.set(primiteCollection, complectCollection, body);
        assertEq(
                "Made[\"Hello\", \"World\"],[Parameter{requestedIds=null, actionIds=null, lastRequestTime=null, casts=[Hello, World]}],[{casts=[Hello, World]}]",
                greeting);
    }
    
    

    @Test
    public void testEncoding() throws Exception
    {
        String stringWithDifficultCharacters = "#äöüßÖÄÜ\"+<>!§$%&(=&\\)";
        final String result = createAnnotationSimpleProxy().sayHello(stringWithDifficultCharacters);
        final String substring = result.substring(0, stringWithDifficultCharacters.length());
        assertEq(stringWithDifficultCharacters, substring);
    }

    @Test
    public void testPrimitiveString() throws Exception
    {
        String param = "another param";
        final String result = createAnnotationSimpleProxy().sendString(param);
        assertEq(param, result);
    }

    @Test
    public void testDouble() throws Exception
    {
        Double param = new Double(-2.0);
        final Double result = createAnnotationSimpleProxy().sendDouble(param);
        assertEq(param, -1 * result);
    }

    @Test
    public void testPrimDouble() throws Exception
    {
        double param = 12.;
        final double result = createAnnotationSimpleProxy().sendPrimDouble(param);
        assertEq(param, -1 * result);
    }

    @Test
    public void testBoolean() throws Exception
    {
        Boolean param = Boolean.FALSE;
        final Boolean result = createAnnotationSimpleProxy().sendBool(param);
        assertEq(param, !result);
    }

    @Test
    public void testPrimBoolean() throws Exception
    {
        boolean param = true;
        final boolean result = createAnnotationSimpleProxy().sendPrimBool(param);
        assertEq(param, !result);
    }

    @Test
    public void testInt() throws Exception
    {
        Integer param = new Integer(42);
        final Integer result = createAnnotationSimpleProxy().sendInt(param);
        assertEq(param, -1 * result);
    }

    @Test
    public void testPrimInt() throws Exception
    {
        int param = 24;
        final int result = createAnnotationSimpleProxy().sendPrimInt(param);
        assertEq(param, -1 * result);
    }
    @Test
    public void testChar() throws Exception
    {
        Character param = new Character('a');
        final Character result = createAnnotationSimpleProxy().sendChar(param);
        assertEq(new Character('b'), result);
    }
}
