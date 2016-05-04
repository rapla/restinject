package org.rapla.client.gwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Singleton;

import org.junit.Test;
import org.rapla.client.AbstractProxyTest;
import org.rapla.common.ExampleService;
import org.rapla.common.ExampleService_GwtJsonProxy;
import org.rapla.common.ExampleSimpleService;
import org.rapla.common.ExampleSimpleService_GwtJsonProxy;
import org.rapla.common.ComponentStarter;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.client.gwt.GwtCommandScheduler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import dagger.Component;

public class GwtProxyTest extends GWTTestCase
{

    AbstractProxyTest genericTest = new AbstractProxyTest()
    {

        @Override
        protected CustomConnector createConnector()
        {
            return new GwtCustomConnector();
        }

        @Override
        protected ExampleService createExampleServiceProxy()
        {
            return new ExampleService_GwtJsonProxy(connector);
        }

        @Override
        protected ExampleSimpleService createExampleSimpleServiceProxy()
        {
            return new ExampleSimpleService_GwtJsonProxy(connector);
        }

        @Override
        public void assertEq(Object o1, Object o2)
        {
            GWTTestCase.assertEquals(o1, o2);
        }

        @Override
        public void fail_(String message)
        {
            GWTTestCase.fail(message);
        }
    };

    /**
     * Specifies a module to use when running this test case. The returned
     * module must include the source for this class.
     *
     * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
     */
    @Override
    public String getModuleName()
    {
        return "org.rapla.GwtTest";
    }

    @Component(modules = {org.rapla.client.gwt.dagger.DaggerRaplaGwtModule.class, org.rapla.client.dagger.DaggerRaplaClientModule.class,org.rapla.common.dagger.DaggerRaplaCommonModule.class })
    @Singleton
    public interface BootstrapInterface
    {
        Bootstrap getBootstrap();
    }

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        genericTest.setUp();
    }

    public void testEncoding() throws Exception
    {
        genericTest.testEncoding();
    }

    public void test() throws Exception
    {
        genericTest.test();
    }

    public void testChunk() throws Exception
    {
        genericTest.testChunk();
    }

    public void testDate() throws Exception
    {
        genericTest.testDate();
    }

    public void test3() throws Exception
    {
        genericTest.test3();
    }

    public void test4() throws Exception
    {
        genericTest.test4();
    }

    public void testException() throws Exception
    {
        genericTest.testException();
    }

    public void testListOfStrings() throws Exception
    {
        genericTest.testListOfStrings();
    }

    public void testCollections() throws Exception
    {
        genericTest.testCollections();
    }

    public void testPrimitiveString() throws Exception
    {
        genericTest.testPrimitiveString();
    }

    public void testLists() throws Exception
    {
        genericTest.testLists();
    }

    public void testSets() throws Exception
    {
        genericTest.testSets();
    }

    public void testArrays() throws Exception
    {
        genericTest.testArrays();
    }

    public void testDouble() throws Exception
    {
        genericTest.testDouble();
    }

    public void testPrimDouble() throws Exception
    {
        genericTest.testPrimDouble();
    }

    public void testBoolean() throws Exception
    {
        genericTest.testBoolean();
    }

    public void testPrimBoolean() throws Exception
    {
        genericTest.testPrimBoolean();
    }

    public void testInt() throws Exception
    {
        genericTest.testInt();
    }

    public void testPrimInt() throws Exception
    {
        genericTest.testPrimInt();
    }
    
    public void testChar() throws Exception
    {
        genericTest.testChar();
    }

    public void testGwtCall() throws Exception
    {

        final Bootstrap bootstrap = createBootstrap();
        ExampleService.Parameter p = new ExampleService.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final ExampleService.Result result = bootstrap.call(p);
        //final FutureResult<ExampleService.Result> futureResult = new ExampleServiceImpl().sayHello(p);
        //ExampleService.Result result = futureResult.get();
        final Collection<String> ids = result.getIds();
        assertEquals(2, ids.size());
        final Iterator<String> iterator = ids.iterator();
        assertEquals("1", iterator.next());
        assertEquals("2", iterator.next());
    }

    private Bootstrap createBootstrap()
    {
        return DaggerGwtProxyTest_BootstrapInterface.create().getBootstrap();
    }

    ExampleService.Result asyncResult;

    public void testGwtPromiseCall() throws Exception
    {
        final Bootstrap bootstrap = createBootstrap();
        ExampleService.Parameter p = new ExampleService.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        CommandScheduler scheduler = new GwtCommandScheduler()
        {
            @Override
            protected void warn(String message, Exception e)
            {
                e.printStackTrace(System.err);
                System.err.println(message);
            }

        };
        final Promise<Collection<ExampleService.Result>> promise = bootstrap.callAsync(p, scheduler);
        delayTestFinish(10000);
        promise.thenAccept((list) ->
        {
            asyncResult = list.iterator().next();
            final Collection<String> ids = asyncResult.getIds();
            assertEquals(2, ids.size());
            final Iterator<String> iterator = ids.iterator();
            assertEquals("1", iterator.next());
            assertEquals("2", iterator.next());
            finishTest();
        }).exceptionally((ex) ->
        {
            ex.printStackTrace();
            fail(ex.getMessage());
            return null;
        });
    }

    public void testStartGeneratedServerComponent()
    {
        System.out.println("Before GWT create");
        ComponentStarter starter = GWT.create(ComponentStarter.class);
        System.out.println("After GWT create " + starter);
        assertEquals("gwt", starter.start());
    }

}