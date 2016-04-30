package org.rapla.rest.client.swing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.rapla.rest.JsonParserWrapper;
import org.rapla.rest.client.RemoteConnectException;
import org.rapla.rest.client.SerializableExceptionInformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaJsonSerializer
{
    private GsonBuilder gsonBuilder = null;
    private final Class resultType;
    private final Class container;

    public JavaJsonSerializer(final Class resultType, final Class container)
    {
        this.resultType = resultType;
        this.container = container;
    }

    private synchronized Gson createGson()
    {
        if (gsonBuilder == null)
            gsonBuilder = JsonParserWrapper.defaultGsonBuilder().disableHtmlEscaping();
        return gsonBuilder.create();
    }

    public String serializeArgument(Object arg)
    {
        final String result;
        if (arg != null)
        {
            Gson gson = createGson();
            result = gson.toJson(arg);
        }
        else
        {
            result = "";
        }
        return result;
    }

    public Object deserializeResult(String unparsedResult) throws RemoteConnectException
    {
        if (resultType.equals(void.class))
        {
            return null;
        }

        JsonParser jsonParser = new JsonParser();
        final JsonElement resultElement;
        try
        {
            resultElement = jsonParser.parse(unparsedResult);
        }
        catch (JsonParseException ex)
        {
            throw new RemoteConnectException("Unparsable json " + ex.getMessage() + ":" + unparsedResult);
        }

        Object resultObject;
        Gson gson = createGson();
        if (container != null && !Object.class.equals(container))
        {
            if (List.class.equals(container) || Collection.class.equals(container) || Set.class.equals(container))
            {
                if (!resultElement.isJsonArray())
                {
                    throw new RemoteConnectException("Array expected as json result");
                }
                Collection<Object> result = Set.class.equals(container) ? new LinkedHashSet<>(): new ArrayList<>();
                JsonArray list = resultElement.getAsJsonArray();
                for (JsonElement element : list)
                {
                    Object obj = gson.fromJson(element, resultType);
                    result.add(obj);
                }
                resultObject = result;
            }
            else if (Map.class.equals(container))
            {
                if (!resultElement.isJsonObject())
                {
                    throw new RemoteConnectException("JsonObject expected as json result");
                }
                final JsonObject map = resultElement.getAsJsonObject();
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                for (Map.Entry<String, JsonElement> entry : map.entrySet())
                {
                    String key = entry.getKey();
                    JsonElement element = entry.getValue();
                    Object obj = gson.fromJson(element, resultType);
                    result.put(key, obj);
                }
                resultObject = result;
            }
            else
            {
                throw new RemoteConnectException("List,Set or Map expected as json container");
            }
        }
        else
        {
            resultObject = gson.fromJson(resultElement, resultType);
        }
        return resultObject;
    }

    public SerializableExceptionInformation deserializeException(String unparsedErrorString)
    {

        JsonElement errorElement;
        try
        {
            JsonParser jsonParser = new JsonParser();
            errorElement = jsonParser.parse(unparsedErrorString);
        }
        catch (JsonParseException ex)
        {
            return null;
        }
        if (!errorElement.isJsonObject())
        {
            return null;
        }
        final Gson jsonMapper = createGson();
        return jsonMapper.fromJson(errorElement, SerializableExceptionInformation.class);
    }
}
