package org.rapla.jsonrpc.client.swing;

import com.google.gson.*;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class HTTPJsonConnector extends HTTPConnector
{

    public HTTPJsonConnector() {
        super();
    }

    public JsonElement sendPost(URL methodURL, JsonElement jsonObject, String authenticationToken, Map<String, String>additionalHeaders) throws IOException,JsonParseException {
        return sendCall("POST", methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    public JsonElement sendGet(URL methodURL, String authenticationToken, Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("GET", methodURL, null, authenticationToken, additionalHeaders);
    }

    public JsonElement sendPut(URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("PUT", methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    public JsonElement sendPatch(URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("PATCH", methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    public JsonElement sendDelete(URL methodURL, String authenticationToken,Map<String, String>additionalHeaders) throws IOException,JsonParseException  {
        return sendCall("DELETE", methodURL, null, authenticationToken, additionalHeaders);
    }

    protected JsonElement sendCall(String requestMethod, URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws  IOException,JsonParseException   {
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
        String resultString = sendCallWithString(requestMethod, methodURL, body, authenticationToken, additionalHeaders);
        JsonElement resultMessage = parseJson(resultString);
        return resultMessage;
    }

   
    
    public JsonElement parseJson(String resultString) throws JsonParseException {
        JsonParser jsonParser = new JsonParser();
        JsonElement  parsed = jsonParser.parse(resultString);
        return parsed;
        
    }


}