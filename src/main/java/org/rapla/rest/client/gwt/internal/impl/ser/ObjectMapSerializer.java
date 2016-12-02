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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.rapla.rest.client.gwt.internal.impl.JsonSerializer;
import org.rapla.rest.client.gwt.internal.impl.ResultDeserializer;

/**
 * Serialization for a {@link Map} using any Object key.
 * <p>
 * The JSON format is an array with even length, alternating key and value
 * elements. For example: <code>[k1, v1, k2, v2, k3, v3, ...]</code>. The keys
 * and values may be any Object type.
 * <p>
 * When deserialized from JSON the Map implementation is always a
 * {@link HashMap}. When serializing to JSON any Map is permitted.
 */
public class ObjectMapSerializer<K, V> extends
    JsonSerializer<Map<K, V>> implements
    ResultDeserializer<Map<K, V>> {
  private final Provider<JsonSerializer<K>> keySerializer;
  private final Provider<JsonSerializer<V>> valueSerializer;

  public ObjectMapSerializer(final JsonSerializer<K> k,
          final JsonSerializer<V> v) {
      keySerializer = new SimpleGwtProvider<JsonSerializer<K>>(k);
      valueSerializer = new SimpleGwtProvider<JsonSerializer<V>>(v);
  }

  
  public ObjectMapSerializer(final Provider<JsonSerializer<K>> k,
      final Provider<JsonSerializer<V>> v) {
    keySerializer = k;
    valueSerializer = v;
  }

  @Override
  public void printJson(final StringBuilder sb, final Map<K, V> o) {
    sb.append('[');
    boolean first = true;
    for (final Map.Entry<K, V> e : o.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      encode(sb, keySerializer.get(), e.getKey());
      sb.append(',');
      encode(sb, valueSerializer.get(), e.getValue());
    }
    sb.append(']');
  }

  private static <T> void encode(final StringBuilder sb,
      final JsonSerializer<T> serializer, final T item) {
    if (item != null) {
      serializer.printJson(sb, item);
    } else {
      sb.append(JS_NULL);
    }
  }

  @Override
  public Map<K, V> fromJson(final Object o) {
    if (o == null) {
      return null;
    }

    final int n = size(o);
    final HashMap<K, V> r = new LinkedHashMap<K, V>();
    for (int i = 0; i < n;) {
      final K k = keySerializer.get().fromJson(get(o, i++));
      final V v = valueSerializer.get().fromJson(get(o, i++));
      r.put(k, v);
    }
    return r;
  }

}
