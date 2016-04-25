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

package org.rapla.rest.generator.internal;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public class JavaClientProxyCreator extends AbstractClientProxyCreator
{
    public static final String PROXY_SUFFIX = "_JavaJsonProxy";

    public JavaClientProxyCreator(final TypeElement remoteService, ProcessingEnvironment processingEnvironment, String generatorName)
    {
        super(remoteService,processingEnvironment, generatorName);
    }


    @Override
    public String getProxySuffix()
    {
        return PROXY_SUFFIX;
    }



}
