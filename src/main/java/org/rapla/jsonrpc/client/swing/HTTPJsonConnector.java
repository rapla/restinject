package org.rapla.jsonrpc.client.swing;

import com.google.gson.*;

import java.io.IOException;
import java.net.URL;

public class HTTPJsonConnector extends HTTPConnector
{

    public HTTPJsonConnector() {
        super();
    }

    public JsonObject sendPost(URL methodURL, JsonElement jsonObject, String authenticationToken) throws IOException,JsonParseException {
        return sendCall("POST", methodURL, jsonObject, authenticationToken);
    }

    public JsonObject sendGet(URL methodURL, String authenticationToken) throws IOException,JsonParseException  {
        return sendCall("GET", methodURL, null, authenticationToken);
    }

    public JsonObject sendPut(URL methodURL, JsonElement jsonObject, String authenticationToken) throws IOException,JsonParseException  {
        return sendCall("PUT", methodURL, jsonObject, authenticationToken);
    }

    public JsonObject sendPatch(URL methodURL, JsonElement jsonObject, String authenticationToken) throws IOException,JsonParseException  {
        return sendCall("PATCH", methodURL, jsonObject, authenticationToken);
    }

    public JsonObject sendDelete(URL methodURL, String authenticationToken) throws IOException,JsonParseException  {
        return sendCall("DELETE", methodURL, null, authenticationToken);
    }

    protected JsonObject sendCall(String requestMethod, URL methodURL, JsonElement jsonObject, String authenticationToken) throws  IOException,JsonParseException   {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String body = gson.toJson( jsonObject);
        String resultString = sendCallWithString(requestMethod, methodURL, body, authenticationToken);
        JsonObject resultMessage = parseJson(resultString);
        return resultMessage;
    }

   
    
    public JsonObject parseJson(String resultString) throws JsonParseException {
        JsonParser jsonParser = new JsonParser();
        JsonElement  parsed = jsonParser.parse(resultString);
        if ( !(parsed instanceof JsonObject))
        {
        	throw new JsonParseException("Invalid json result. JsonObject expected." + resultString);
        }
        JsonObject resultMessage = (JsonObject) parsed;
        return resultMessage;
        
    }


}