package org.rapla.gwtjsonrpc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker interface for JSON based RPC. Should be replaced with a marker annotation when generator supports annotations
 * <p>
 * Application service interfaces should extend this interface:
 *
 * <pre>
 * public interface FooService extends RemoteJsonService ...
 * </pre>
 * <p>
 * and declare each method as returning void and accepting {@link org.rapla.gwtjsonrpc.common.AsyncCallback}
 * as the final parameter, with a concrete type specified as the result type:
 *
 * <p>
 * Instances of the interface can be obtained in the gwt and configured to
 * reference a particular JSON server:
 *
 * <pre>
 * FooService mysvc = GWT.create(FooService.class);
 * ((ServiceDefTarget) mysvc).setServiceEntryPoint(GWT.getModuleBaseURL()
 *     + &quot;FooService&quot;);
 *</pre>
 * <p>
 * Calling conventions match the JSON-RPC 1.1 working draft from 7 August 2006
 * (<a href="http://json-rpc.org/wd/JSON-RPC-1-1-WD-20060807.html">draft</a>).
 * Only positional parameters are supported.
 * <p>
 * JSON service callbacks may also be declared; see
 * {@link org.rapla.gwtjsonrpc.client.CallbackHandle}.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteJsonMethod
{
    String path() default "";
}
