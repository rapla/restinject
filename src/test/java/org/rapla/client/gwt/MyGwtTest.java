package org.rapla.client.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import org.rapla.gwtjsonrpc.annotation.AnnotationProcessingTest;
import org.rapla.gwtjsonrpc.client.impl.AbstractJsonProxy;
import org.rapla.gwtjsonrpc.client.impl.EntryPointFactory;

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
  @Override
  public String getModuleName() {
    return "org.rapla.GwtTest";
  }

  public void testGwtCall()
  {
    AbstractJsonProxy.setServiceEntryPointFactory(new EntryPointFactory()
    {
      @Override public String getEntryPoint(String interfaceName, String relativePath)
      {
        String url = GWT.getModuleBaseURL() + "rapla/json/"+ (relativePath != null ? relativePath : interfaceName);
        return url;
      }
    });
    final MainInjector injector = GWT.create(MainInjector.class);
    final Bootstrap bootstrap = injector.get();
    try
    {
      AnnotationProcessingTest.Parameter p = new AnnotationProcessingTest.Parameter();
      p.setActionIds(Arrays.asList(new Integer[] {1,2}));
      final AnnotationProcessingTest.Result result = bootstrap.call(p);
      //final FutureResult<AnnotationProcessingTest.Result> futureResult = new AnnotationProcessingTestImpl().sayHello(p);
      //AnnotationProcessingTest.Result result = futureResult.get();
      final List<String> ids = result.getIds();
      assertEquals( 2,ids.size());
      assertEquals( "1", ids.get(0));
      assertEquals( "2", ids.get(1));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}