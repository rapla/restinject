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

import javax.inject.Provider;
import java.util.ArrayList;

/**
 * Serialization for a {@link java.util.List}.
 * <p>
 * When deserialized from JSON the List implementation is always an
 * {@link ArrayList}. When serializing to JSON any List is permitted.
 */
public class ListSerializer<T> extends JsonSerializer<java.util.List<T>>
    implements ResultDeserializer<java.util.List<T>> {
  private final Provider<JsonSerializer<T>> serializer;

  public ListSerializer(final JsonSerializer<T> s) {
      serializer = new SimpleGwtProvider<JsonSerializer<T>>(s);
  }
  
  public ListSerializer(final Provider<JsonSerializer<T>> s) {
    serializer = s;
  }

  @Override
  public void printJson(final StringBuilder sb, final java.util.List<T> o) {
    sb.append('[');
    boolean first = true;
    for (final T item : o) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      if (item != null) {
        serializer.get().printJson(sb, item);
      } else {
        sb.append(JS_NULL);
      }
    }
    sb.append(']');
  }

  @Override
  public java.util.List<T> fromJson(final Object o) {
    if (o == null) {
      return null;
    }
    final int n = size(o);
    final ArrayList<T> r = new ArrayList<T>(n);
    for (int i = 0; i < n; i++) {
      r.add(serializer.get().fromJson(get(o, i)));
    }
    return r;
  }
}
