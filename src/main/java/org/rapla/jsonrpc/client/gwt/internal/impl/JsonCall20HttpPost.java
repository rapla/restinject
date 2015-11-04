// Copyright 2009 Gert Scholten
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

package org.rapla.jsonrpc.client.gwt.internal.impl;

import org.rapla.jsonrpc.common.internal.JsonConstants;

import com.google.gwt.http.client.RequestBuilder;
import org.rapla.jsonrpc.client.gwt.AbstractJsonProxy;

/** JsonCall implementation for JsonRPC version 2.0 over HTTP POST */
public class JsonCall20HttpPost<T> extends JsonCall<T> {
  public JsonCall20HttpPost(AbstractJsonProxy abstractJsonProxy,
      String methodName, String requestParams,
      ResultDeserializer<T> resultDeserializer) {
    super(abstractJsonProxy, methodName, requestParams, resultDeserializer);
  }

  @Override
  protected void send() {
    requestId = ++lastRequestId;
    final StringBuilder body = new StringBuilder();
    body.append("{\"jsonrpc\":\"2.0\",\"method\":\"");
    body.append(methodName);
    body.append("\",\"params\":");
    body.append(requestParams);
    body.append(",\"id\":").append(requestId);
    body.append("}");

    final RequestBuilder rb;
    rb = new RequestBuilder(RequestBuilder.POST, proxy.getServiceEntryPoint());
    rb.setHeader("Content-Type", JsonConstants.JSONRPC20_REQ_CT);
    rb.setHeader("Accept", JsonConstants.JSONRPC20_ACCEPT_CTS);
    rb.setCallback(this);
    rb.setRequestData(body.toString());

    send(rb);
  }
}
