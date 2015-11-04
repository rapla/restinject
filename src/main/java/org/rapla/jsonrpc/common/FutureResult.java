package org.rapla.jsonrpc.common;


public interface FutureResult<T> {
	public T get() throws Exception;
	public void get(AsyncCallback<T> callback);
}
