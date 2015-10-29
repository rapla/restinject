package org.rapla.client.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import dagger.Component;
import org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest;
import org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTestImpl;
import org.rapla.gwtjsonrpc.client.impl.AbstractJsonProxy;
import org.rapla.gwtjsonrpc.client.impl.EntryPointFactory;
import org.rapla.gwtjsonrpc.common.FutureResult;

import java.util.Arrays;
import java.util.List;

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
    
    @Component(modules=org.rapla.dagger.DaggerGwtModule.class)
    public interface BootstrapInterface {
        Bootstrap getBootstrap();
    }

    public void testGwtCall() throws Exception
    {
        AbstractJsonProxy.setServiceEntryPointFactory(new EntryPointFactory()
        {
            @Override public String getEntryPoint(String interfaceName, String relativePath)
            {
                String url = GWT.getModuleBaseURL() + "rapla/json/" + (relativePath != null ? relativePath : interfaceName);
                return url;
            }
        });

        final Bootstrap bootstrap = org.rapla.client.gwt.DaggerMyGwtTest_BootstrapInterface.create().getBootstrap();
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
}