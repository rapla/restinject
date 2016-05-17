package org.rapla.rest.client.gwt.internal.impl;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

@jsinterop.annotations.JsType(isNative = true,name = "XMLHttpRequest",namespace = JsPackage.GLOBAL)
public interface XMLHttpRequest
{
//    public int status;
//    public int readyState;
//    public String responseText;
//    public String statusText;
//    public Onreadystatechange onreadystatechange;
    public  void setRequestHeader(String s, String applicationJson);
    public  void send(String requestData);
    public  String getResponseHeader(String s);
    public  void open(String httpMethod, String url, boolean asynchronous);

    @JsFunction
    @FunctionalInterface
    interface Onreadystatechange
    {
        public void run();
    }

    @JsProperty
    public int getStatus();
//    {
//        return status;
//    }

    @JsProperty
    public String getResponseText();
//    {
//        return responseText;
//    }

    @JsProperty
    public String getStatusText();
//    {
//        return statusText;
//    }

    @JsProperty
    public int getReadyState();
//    {
//        return readyState;
//    }

    @JsProperty
    public void setOnreadystatechange(Onreadystatechange onreadystatechange);
//    {
//        this.onreadystatechange = onreadystatechange;
//    }

    @JsProperty
    public Onreadystatechange getOnreadystatechange();
//    {
//        return onreadystatechange;
//    }

}
