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

import javax.inject.Provider;

import org.rapla.rest.client.gwt.internal.impl.JsonSerializer;

/**
 * Default serialization for any Object[] sort of type.
 * <p>
 * Primitive array types (like <code>int[]</code>) are not supported.
 */
public class ObjectArraySerializer<T> {
  private final Provider<? extends JsonSerializer<T>> serializer;
  
  public ObjectArraySerializer(final JsonSerializer<T> s) {
      serializer = new SimpleGwtProvider<JsonSerializer<T>>(s);
  }

  public ObjectArraySerializer(final Provider<? extends JsonSerializer<T>> s) {
    serializer = s;
  }

  public void printJson(final StringBuilder sb, final T[] o) {
    sb.append('[');
    for (int i = 0, n = o.length; i < n; i++) {
      if (i > 0) {
        sb.append(',');
      }
      final T v = o[i];
      if (v != null) {
        serializer.get().printJson(sb, v);
      } else {
        sb.append(JsonSerializer.JS_NULL);
      }
    }
    sb.append(']');
  }

  public void fromJson(final Object jso, final T[] r) {
    for (int i = 0; i < r.length; i++) {
      r[i] = serializer.get().fromJson(get(jso, i));
    }
  }

  public static native int size(Object o)/*-{ return o.length; }-*/;

  private static final native Object get(Object o, int i)/*-{ return o[i]; }-*/;
}
