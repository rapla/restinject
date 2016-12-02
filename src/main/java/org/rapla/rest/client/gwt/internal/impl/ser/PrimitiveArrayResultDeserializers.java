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

import org.rapla.rest.client.gwt.internal.impl.ResultDeserializer;

public class PrimitiveArrayResultDeserializers  {
  public static ResultDeserializer<Boolean[]> BOOLEAN_INSTANCE =
      new ResultDeserializer<Boolean[]>() {
        @Override
        public Boolean[] fromJson(Object responseObject) {
          final Boolean[] tmp = new Boolean[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Byte[]> BYTE_INSTANCE =
      new ResultDeserializer<Byte[]>() {
        @Override
        public Byte[] fromJson(Object responseObject) {
          final Byte[] tmp = new Byte[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Character[]> CHARACTER_INSTANCE =
      new ResultDeserializer<Character[]>() {
        @Override
        public Character[] fromJson(Object responseObject) {
          final Character[] tmp = new Character[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Double[]> DOUBLE_INSTANCE =
      new ResultDeserializer<Double[]>() {
        @Override
        public Double[] fromJson(Object responseObject) {
          final Double[] tmp = new Double[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Float[]> FLOAT_INSTANCE =
      new ResultDeserializer<Float[]>() {
        @Override
        public Float[] fromJson(Object responseObject) {
          final Float[] tmp = new Float[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Integer[]> INTEGER_INSTANCE =
      new ResultDeserializer<Integer[]>() {
        @Override
        public Integer[] fromJson(Object responseObject) {
          final Integer[] tmp = new Integer[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(getResult(responseObject),
              tmp);
          return tmp;
        }
      };
  public static ResultDeserializer<Short[]> SHORT_INSTANCE =
      new ResultDeserializer<Short[]>() {
        @Override
        public Short[] fromJson(Object responseObject) {
          final Short[] tmp = new Short[getResultSize(responseObject)];
          PrimitiveArraySerializer.INSTANCE.fromJson(responseObject,
              tmp);
          return tmp;
        }
      };

    protected static native Object getResult(Object result)
  /*-{
    return result;
  }-*/;

    protected static native int getResultSize(Object result)
  /*-{
    return result.length;
  }-*/;
}
