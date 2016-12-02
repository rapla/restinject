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

package org.rapla.rest.client.gwt.internal.impl.ser;

import org.rapla.rest.client.gwt.internal.impl.JsonSerializer;
import org.rapla.rest.client.gwt.internal.impl.ResultDeserializer;

/** Default serialization for a String. */
public final class JavaLangInteger_JsonSerializer extends
    JsonSerializer<Integer> implements
    ResultDeserializer<Integer> {
  public static final JavaLangInteger_JsonSerializer INSTANCE =
      new JavaLangInteger_JsonSerializer();
  public static final javax.inject.Provider<JsonSerializer<Integer>> INSTANCE_PROVIDER = new javax.inject.Provider<JsonSerializer<Integer>>(){
      public JsonSerializer<Integer> get(){return INSTANCE;}
  };

  @Override
  public Integer fromJson(final Object o) {
    return (Integer) o;
  }

  @Override
  public void printJson(final StringBuilder sb, final Integer o) {
    sb.append(o);
  }

}
