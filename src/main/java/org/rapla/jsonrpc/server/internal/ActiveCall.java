// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.rapla.jsonrpc.server.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rapla.jsonrpc.common.AsyncCallback;

import com.google.gson.JsonElement;

/** An active RPC call. */
public class ActiveCall implements AsyncCallback<Object> {
  public final HttpServletRequest httpRequest;
  public final HttpServletResponse httpResponse;
  public JsonElement id;
  public String versionName;
  public JsonElement versionValue;
  public MethodHandle method;
  public Object[] params;
  public Object result;
  public Throwable externalFailure;
  public Throwable internalFailure;
  
  /**
   * Create a new call.
   *
   * @param req the request.
   * @param resp the response.
   */
  public ActiveCall(final HttpServletRequest req, final HttpServletResponse resp) {
    httpRequest = req;
    httpResponse = resp;
  }


  /**
   * @return true if this call has something to send to the gwt; false if the
   *         call still needs to be computed further in order to come up with a
   *         success return value or a failure
   */
  public final boolean isComplete() {
    return result != null || externalFailure != null || internalFailure != null;
  }

  @Override
  public final void onSuccess(final Object result) {
    this.result = result;
    this.externalFailure = null;
    this.internalFailure = null;
  }

  @Override
  public void onFailure(final Throwable error) {
    this.result = null;
    this.externalFailure = error;
    this.internalFailure = null;
  }

  public final void onInternalFailure(final Throwable error) {
    this.result = null;
    this.externalFailure = null;
    this.internalFailure = error;
  }

  /** Mark the response to be uncached by proxies and browsers. */
  public void noCache() {
    httpResponse.setHeader("Expires", "Fri, 01 Jan 1980 00:00:00 GMT");
    httpResponse.setHeader("Pragma", "no-cache");
    httpResponse.setHeader("Cache-Control", "no-cache, must-revalidate");
  }


  public boolean hasFailed() {
      return externalFailure != null || internalFailure != null; 
  }
  
  public HttpServletRequest getHttpRequest() {
    return httpRequest;
  }
  
  public HttpServletResponse getHttpResponse() {
    return httpResponse;
  }
}
