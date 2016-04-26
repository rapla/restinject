// Copyright 2009 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

public class PrimitiveResultDeserializers {

  public static final ResultDeserializer<Boolean> BOOLEAN_INSTANCE =
      new ResultDeserializer<Boolean>() {
        @Override
        public Boolean fromJson(final Object responseObject) {
          return (Boolean)responseObject;
        }
      };
  public static final ResultDeserializer<Byte> BYTE_INSTANCE =
      new ResultDeserializer<Byte>() {
        @Override
        public Byte fromJson(final Object responseObject) {
          return (Byte) responseObject;
        }
      };
  public static final ResultDeserializer<Character> CHARACTER_INSTANCE =
      new ResultDeserializer<Character>() {
        @Override
        public Character fromJson(final Object responseObject) {
          return JsonSerializer.toChar((String) (responseObject));
        }
      };
  public static final ResultDeserializer<Double> DOUBLE_INSTANCE =
      new ResultDeserializer<Double>() {
        @Override
        public Double fromJson(final Object responseObject) {
          return (Double)responseObject;
        }
      };
  public static final ResultDeserializer<Float> FLOAT_INSTANCE =
      new ResultDeserializer<Float>() {
        @Override
        public Float fromJson(final Object responseObject) {
          return (Float)responseObject;
        }
      };
  public static final ResultDeserializer<Integer> INTEGER_INSTANCE =
      new ResultDeserializer<Integer>() {
        @Override
        public Integer fromJson(final Object responseObject) {
          return (Integer)responseObject;
        }
      };
  public static final ResultDeserializer<Short> SHORT_INSTANCE =
      new ResultDeserializer<Short>() {
        @Override
        public Short fromJson(final Object responseObject) {
          return (Short)responseObject;
        }
      };
}
