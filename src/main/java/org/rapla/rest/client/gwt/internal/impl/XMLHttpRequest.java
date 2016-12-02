package org.rapla.rest.client.gwt.internal.impl;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

@jsinterop.annotations.JsType(isNative = true,name = "XMLHttpRequest",namespace = JsPackage.GLOBAL)
public interface XMLHttpRequest
{
    void setRequestHeader(String s, String applicationJson);
    void send(String requestData);
    String getResponseHeader(String s);
    void open(String httpMethod, String url, boolean asynchronous);

    @JsFunction
    @FunctionalInterface
    interface Onreadystatechange
    {
        public void run();
    }

    @JsProperty
    int getStatus();

    @JsProperty
    String getResponseText();

    @JsProperty
    String getStatusText();

    @JsProperty
    int getReadyState();

    @JsProperty
    void setOnreadystatechange(Onreadystatechange onreadystatechange);

    @JsProperty
    Onreadystatechange getOnreadystatechange();

}
