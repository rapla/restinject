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

package org.rapla.gwtjsonrpc.client.impl.ser;

import org.rapla.gwtjsonrpc.client.impl.JsonSerializer;
import org.rapla.gwtjsonrpc.client.impl.ResultDeserializer;

import com.google.gwt.core.client.JavaScriptObject;

/** Base serializer for Enum types. */
public abstract class EnumSerializer<T extends Enum<?>> extends
    JsonSerializer<T> implements ResultDeserializer<Object> {
  @Override
  public void printJson(final StringBuilder sb, final T o) {
    sb.append('"');
    sb.append(o.name());
    sb.append('"');
  }
  @Override
  public Object fromResult(JavaScriptObject responseObject) {
    return fromJson(responseObject.toString());
  }
}
