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

import java.util.Date;

import org.rapla.rest.client.gwt.internal.impl.JsonSerializer;
import org.rapla.rest.client.gwt.internal.impl.ResultDeserializer;

import org.rapla.rest.client.internal.isodate.ISODateTimeFormat;

/** Default serialization for a {@link Date}. */
public final class JavaUtilDate_JsonSerializer extends
    JsonSerializer<Date> implements
    ResultDeserializer<Date> {
  public static final JavaUtilDate_JsonSerializer INSTANCE =
      new JavaUtilDate_JsonSerializer();
  public static final javax.inject.Provider<JavaUtilDate_JsonSerializer> INSTANCE_PROVIDER = new javax.inject.Provider<JavaUtilDate_JsonSerializer>(){
      public JavaUtilDate_JsonSerializer get(){return INSTANCE;} 
  };

  @Override
  public Date fromJson(final Object o) {
    if (o != null) {
    	Date date = ISODateTimeFormat.INSTANCE.parseTimestamp(o.toString());
    	return date;
    }
    return null;
  }

  public void serializeDate(final StringBuilder sb, final Date date)
  {
    if ( date != null)
    {
       sb.append(ISODateTimeFormat.INSTANCE.formatTimestamp(date));
    }

  }

  @Override
  public void printJson(final StringBuilder sb, final Date o) {
    sb.append('"');
    String string = ISODateTimeFormat.INSTANCE.formatTimestamp(o);
    sb.append(string);
    sb.append('"');
  }
}
