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

package org.rapla.gwtjsonrpc.annotation;

import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;

/**
 * Generates proxy implementations of RemoteJsonService.
 */
public class RemoteJsonServiceProxyGenerator extends AbstractProcessor
{
    @Override public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
        return Collections.singleton( RemoteJsonMethod.class.getCanonicalName());
    }


    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (roundEnv.processingOver())
        {
            return false;
        }
        Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(RemoteJsonMethod.class);
        org.rapla.gwtjsonrpc.annotation.TreeLogger proxyLogger = new org.rapla.gwtjsonrpc.annotation.TreeLogger();
        for ( Element element :elementsAnnotatedWith)
        {
            ProxyCreator proxyCreator = new ProxyCreator((TypeElement)element, processingEnv);
            try
            {
                return proxyCreator.create(proxyLogger) != null;
            }
            catch (org.rapla.gwtjsonrpc.annotation.UnableToCompleteException e)
            {
                return false;
            }
        }
        return false;
    }
}
