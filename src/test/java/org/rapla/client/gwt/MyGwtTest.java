package org.rapla.client.gwt;

import java.util.Arrays;
import java.util.List;

import org.rapla.common.AnnotationProcessingTest;
import org.rapla.common.ComponentStarter;
import org.rapla.rest.client.EntryPointFactory;
import org.rapla.rest.client.gwt.AbstractJsonProxy;
import org.rapla.scheduler.CommandScheduler;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.client.gwt.GwtCommandScheduler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import dagger.Component;

public class MyGwtTest extends GWTTestCase
{

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
    public interface BootstrapInterface {
        Bootstrap getBootstrap();
    }

    @Override protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();
        AbstractJsonProxy.setServiceEntryPointFactory(new EntryPointFactory()
        {
            @Override public String getEntryPoint(String interfaceName, String relativePath)
            {
                String moduleBase = GWT.getModuleBaseURL();//.replaceAll("/org.rapla.GwtTest.JUnit","");
                int port = 59683;
//                try
//                {
//                    port = new URL(s).getPort();
//                }
//                catch ( Exception ex)
//                {
//                }
//                final String s1 = "http://192.168.0.102:" + port + "/rest/AnnotationProcessingTest";
//                System.out.println("Entry point " + s1 );
//                return s1;

                String s= moduleBase + "rest/";
                String url = s + (relativePath != null ? relativePath : interfaceName);
                System.out.println("module base '" + moduleBase + "', entry point " + url + " for relativPath " + relativePath + " and interface " + interfaceName);
                return url;

            }
        });

    }

    public void testGwtCall() throws Exception
    {

        final Bootstrap bootstrap = DaggerMyGwtTest_BootstrapInterface.create().getBootstrap();
        AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
        p.setActionIds(Arrays.asList(new Integer[] { 1, 2 }));
        final AnnotationProcessingTest.Result result = bootstrap.call(p);
        //final FutureResult<AnnotationProcessingTest.Result> futureResult = new AnnotationProcessingTestImpl().sayHello(p);
        //AnnotationProcessingTest.Result result = futureResult.get();
        final List<String> ids = result.getIds();
        assertEquals(2, ids.size());
        assertEquals("1", ids.get(0));
        assertEquals("2", ids.get(1));
    }

    AnnotationProcessingTest.Result asyncResult;

    public void testGwtPromiseCall() throws Exception
    {
        final Bootstrap bootstrap = DaggerMyGwtTest_BootstrapInterface.create().getBootstrap();
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
        final Promise<List<AnnotationProcessingTest.Result>> promise = bootstrap.callAsync(p, scheduler);
        delayTestFinish(10000);
        promise.thenAccept((list) -> {
            asyncResult = list.get(0);
            final List<String> ids = asyncResult.getIds();
            assertEquals(2, ids.size());
            assertEquals("1", ids.get(0));
            assertEquals("2", ids.get(1));
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