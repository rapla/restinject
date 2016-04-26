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

package org.rapla.rest.client;

public abstract class AbstractJsonProxy
{
    /** URL of the service implementation. */
    private String path;
    final protected CustomConnector connector;

    public AbstractJsonProxy(CustomConnector connector)
    {
        this.connector = connector;
    }

    protected void setPath(String path)
    {
        this.path = path;
    }

    protected String getPath()
    {
        return path;
    }

    protected String getMethodUrl( String subPath)
    {
        String contextPath = getPath() + (subPath == null || subPath.isEmpty() ? "" : (subPath.startsWith("?") ? "" : "/") + subPath);
        final String entryPoint = connector.getFullQualifiedUrl(contextPath);
        return entryPoint;
    }

}