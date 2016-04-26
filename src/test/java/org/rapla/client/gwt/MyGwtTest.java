package org.rapla.client.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import dagger.Component;
import org.rapla.client.AbstractProxyTest;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.AnnotationProcessingTest_GwtJsonProxy;
import org.rapla.common.AnnotationSimpleProcessingTest;
import org.rapla.common.AnnotationSimpleProcessingTest_GwtJsonProxy;
import org.rapla.common.ComponentStarter;
import org.rapla.rest.client.CustomConnector;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.client.gwt.GwtCommandScheduler;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class MyGwtTest extends GWTTestCase
{

    AbstractProxyTest genericTest = new AbstractProxyTest()
    {

        @Override protected CustomConnector createConnector()
        {
            return new GwtCustomConnector();
        }

        @Override protected AnnotationProcessingTest createAnnotationProcessingProxy()
        {
            return new AnnotationProcessingTest_GwtJsonProxy(connector);
        }

        @Override protected AnnotationSimpleProcessingTest createAnnotationSimpleProxy()
        {
            return new AnnotationSimpleProcessingTest_GwtJsonProxy(connector);
        }

        @Override public void assertEq(Object o1, Object o2)
        {
            GWTTestCase.assertEquals( o1, o2);
        }

        @Override public void fail_(String message)
        {
            GWTTestCase.fail( message);
        }
    };

    /**
     * Specifies a module to use when running this test case. The returned
     * module must include the source for this class.
     *
     * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
     */
    @Override public String getModuleName()
    {
        return "org.rapla.GwtTest";
    }
    
    @Component(modules= org.rapla.client.gwt.dagger.DaggerRaplaGwtModule.class)
    @Singleton
    public interface BootstrapInterface {
        Bootstrap getBootstrap();
    }

    @Override protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
    }

    public void testCollections() throws Exception
    {
        genericTest.testCollections();
    }

    public void testGwtCall() throws Exception
    {

        final Bootstrap bootstrap = createBootstrap();
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final AnnotationProcessingTest.Result result = bootstrap.call(p);
        //final FutureResult<AnnotationProcessingTest.Result> futureResult = new AnnotationProcessingTestImpl().sayHello(p);
        //AnnotationProcessingTest.Result result = futureResult.get();
        final Collection<String> ids = result.getIds();
        assertEquals(2, ids.size());
        final Iterator<String> iterator = ids.iterator();
        assertEquals("1", iterator.next());
        assertEquals("2", iterator.next());
    }

    private Bootstrap createBootstrap()
    {
        return DaggerMyGwtTest_BootstrapInterface.create().getBootstrap();
    }

    AnnotationProcessingTest.Result asyncResult;

    public void testGwtPromiseCall() throws Exception
    {
        final Bootstrap bootstrap = createBootstrap();
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        CommandScheduler scheduler = new GwtCommandScheduler()
        {
            @Override protected void warn(String message, Exception e)
            {
                e.printStackTrace( System.err);
                System.err.println( message);
            }

        };
        final Promise<Collection<AnnotationProcessingTest.Result>> promise = bootstrap.callAsync(p, scheduler);
        delayTestFinish(10000);
        promise.thenAccept((list) -> {
            asyncResult = list.iterator().next();
            final Collection<String> ids = asyncResult.getIds();
            assertEquals(2, ids.size());
            final Iterator<String> iterator = ids.iterator();
            assertEquals("1", iterator.next());
            assertEquals("2", iterator.next());
            finishTest();
        }).exceptionally((ex)->{ex.printStackTrace();fail(ex.getMessage());return null;});
    }

    public void testStartGeneratedServerComponent()
    {
        System.out.println("Before GWT create");
        ComponentStarter starter = GWT.create(ComponentStarter.class);
        System.out.println("After GWT create "+starter);
        assertEquals("gwt", starter.start());
    }

}