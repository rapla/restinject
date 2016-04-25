package org.rapla.rest.client.swing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.RaplaConnectException;
import org.rapla.rest.client.SerializableExceptionInformation;

import javax.inject.Provider;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
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
    private final Provider<CustomConnector> customConnectorProvider;

    public JavaJsonSerializer(final Provider<CustomConnector> customConnector, final Class resultType, final Class container)
    {
        this.resultType = resultType;
        this.customConnectorProvider = customConnector;
        this.container = container;
    }

    private synchronized Gson createGson()
    {
        if (gsonBuilder == null)
            gsonBuilder = JSONParserWrapper.defaultGsonBuilder(customConnectorProvider.get().getNonPrimitiveClasses()).disableHtmlEscaping();
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

    public String serializeArgumentUrl(Object arg)
    {
        final String result = serializeArgument(arg);
        try
        {
            return URLEncoder.encode(result, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return result;
        }
    }

    private Object deserializeReturnValue(Class<?> returnType, JsonElement element)
    {
        Gson gson = createGson();

        Object result = gson.fromJson(element, returnType);
        return result;
    }

    private List deserializeReturnList(Class<?> returnType, JsonArray list)
    {
        Gson gson = createGson();
        List<Object> result = new ArrayList<Object>();
        for (JsonElement element : list)
        {
            Object obj = gson.fromJson(element, returnType);
            result.add(obj);
        }
        return result;
    }

    private Set deserializeReturnSet(Class<?> returnType, JsonArray list)
    {
        Gson gson = createGson();
        Set<Object> result = new LinkedHashSet<Object>();
        for (JsonElement element : list)
        {
            Object obj = gson.fromJson(element, returnType);
            result.add(obj);
        }
        return result;
    }

    private Map deserializeReturnMap(Class<?> returnType, JsonObject map)
    {
        Gson gson = createGson();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, JsonElement> entry : map.entrySet())
        {
            String key = entry.getKey();
            JsonElement element = entry.getValue();
            Object obj = gson.fromJson(element, returnType);
            result.put(key, obj);
        }
        return result;
    }

    public Object deserializeResult(String unparsedResult) throws RaplaConnectException
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
            throw new RaplaConnectException("Unparsable json " + ex.getMessage() + ":" + unparsedResult);
        }

        Object resultObject;
        if (container != null)
        {
            if (List.class.equals(container))
            {
                if (!resultElement.isJsonArray())
                {
                    throw new RaplaConnectException("Array expected as json result");
                }
                resultObject = deserializeReturnList(resultType, resultElement.getAsJsonArray());
            }
            else if (Set.class.equals(container))
            {
                if (!resultElement.isJsonArray())
                {
                    throw new RaplaConnectException("Array expected as json result");
                }
                resultObject = deserializeReturnSet(resultType, resultElement.getAsJsonArray());
            }
            else if (Map.class.equals(container))
            {
                if (!resultElement.isJsonObject())
                {
                    throw new RaplaConnectException("JsonObject expected as json result");
                }
                resultObject = deserializeReturnMap(resultType, resultElement.getAsJsonObject());
            }
            else if (Object.class.equals(container))
            {
                resultObject = deserializeReturnValue(resultType, resultElement);
            }
            else
            {
                throw new RaplaConnectException("Array expected as json result");
            }
        }
        else
        {
            resultObject = deserializeReturnValue(resultType, resultElement);
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
