package org.rapla.rest.client.swing;

import com.google.gson.*;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class HTTPJsonConnector extends HTTPConnector
{

    public HTTPJsonConnector() {
        super();
    }

    public CallResult sendPost(URL methodURL, JsonElement jsonObject, String authenticationToken, Map<String, String>additionalHeaders) throws IOException,JsonParseException {
        return sendCall("POST", methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    public CallResult sendGet(URL methodURL, String authenticationToken, Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("GET", methodURL, null, authenticationToken, additionalHeaders);
    }

    public CallResult sendPut(URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("PUT", methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    public CallResult sendPatch(URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("PATCH", methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    public CallResult sendDelete(URL methodURL, String authenticationToken,Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("DELETE", methodURL, null, authenticationToken, additionalHeaders);
    }

    protected CallResult sendCall(String requestMethod, URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws  IOException,JsonParseException   {
        final String body = jsonToString(jsonObject);
        CallResult callResult = sendCallWithString(requestMethod, methodURL, body, authenticationToken, additionalHeaders);
        return callResult;
    }

    protected String jsonToString(JsonElement jsonObject)
    {
        final String body;
        if(jsonObject != null)
        {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            body = gson.toJson( jsonObject);
        }
        else
        {
            body = "";
        }
        return body;
    }

}