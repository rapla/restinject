package org.rapla.jsonrpc.client.swing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;

import org.rapla.jsonrpc.client.EntryPointFactory;
import org.rapla.jsonrpc.client.gwt.MockProxy;
import org.rapla.jsonrpc.common.ExceptionDeserializer;
import org.rapla.jsonrpc.common.AsyncCallback;
import org.rapla.jsonrpc.common.FutureResult;
import org.rapla.jsonrpc.common.internal.JSONParserWrapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BasicRaplaHTTPConnector extends HTTPJsonConnector
{
    private static EntryPointFactory serviceEntryPointFactory;

    //private String clientVersion;
    //CommandScheduler scheduler;
    String path;

    public boolean isMock()
    {
        return customConnector.getMockProxy() != null;
    }

    protected MockProxy getMockProxy()
    {
        return customConnector.getMockProxy();
    }

    protected void setPath(String path)
    {
        this.path = path;
    }

    protected String getPath()
    {
        return path;
    }

    public static EntryPointFactory getServiceEntryPointFactory()
    {
        return serviceEntryPointFactory;
    }

    public static void setServiceEntryPointFactory(EntryPointFactory serviceEntryPointFactory)
    {
        BasicRaplaHTTPConnector.serviceEntryPointFactory = serviceEntryPointFactory;
    }

    final private CustomConnector customConnector;
    protected Executor scheduler;

    protected String getMockAccessToken()
    {
        return customConnector.getAccessToken();
    }
    final GsonBuilder gsonBuilder;
    public BasicRaplaHTTPConnector(CustomConnector customConnector)
    {
        this.scheduler = customConnector.getScheduler();
        this.customConnector = customConnector;
        gsonBuilder = JSONParserWrapper.defaultGsonBuilder(customConnector.getNonPrimitiveClasses()).disableHtmlEscaping();
    }

    public abstract class MyFutureResult<T> implements FutureResult<T>
    {
        public abstract T get() throws Exception;

        @Override public void get(final AsyncCallback callback)
        {
            scheduler.execute(new Runnable()
            {
                public void run()
                {
                    Object result;
                    try
                    {
                        result = get();
                    }
                    catch (Exception e)
                    {
                        callback.onFailure(e);
                        return;
                    }
                    callback.onSuccess(result);
                }

            });
        }
    }

    private JsonElement serializeArguments(Object arg)
    {
        Gson serializer = gsonBuilder.create();
        return serializer.toJsonTree(arg);
    }

    /*
    protected Class[] getNonPrimitiveClasses()
    {
        return new Class[] {};
    }

    protected Exception deserializeException(String classname, String s, List<String> params)
    {
        return new Exception(classname + ": " + s + params.toString());
    }

    protected String reauth(BasicRaplaHTTPConnector proxy) throws Exception
    {

    }
    */

    public interface CustomConnector extends ExceptionDeserializer
    {
        String reauth(BasicRaplaHTTPConnector proxy) throws Exception;
        Exception deserializeException(String classname, String s, List<String> params);
        Class[] getNonPrimitiveClasses();
        Exception getConnectError(IOException ex);
        Executor getScheduler();
        String getAccessToken();
        MockProxy getMockProxy();
    }

    private Gson createJsonMapper()
    {
        Gson gson = gsonBuilder.create();
        return gson;
    }

    private Object deserializeReturnValue(Class<?> returnType, JsonElement element)
    {
        Gson gson = createJsonMapper();

        Object result = gson.fromJson(element, returnType);
        return result;
    }

    private List deserializeReturnList(Class<?> returnType, JsonArray list)
    {
        Gson gson = createJsonMapper();
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
        Gson gson = createJsonMapper();
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
        Gson gson = createJsonMapper();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Entry<String, JsonElement> entry : map.entrySet())
        {
            String key = entry.getKey();
            JsonElement element = entry.getValue();
            Object obj = gson.fromJson(element, returnType);
            result.put(key, obj);
        }
        return result;
    }

    private Exception deserializeExceptionObject(JsonObject result)
    {
        JsonObject errorElement = result.getAsJsonObject("error");
        JsonObject data = errorElement.getAsJsonObject("data");
        JsonElement message = errorElement.get("message");
        @SuppressWarnings("unused") JsonElement code = errorElement.get("code");
        if (data != null)
        {
            JsonArray paramObj = (JsonArray) data.get("params");
            JsonElement jsonElement = data.get("exception");
            JsonElement stacktrace = data.get("stacktrace");
            if (jsonElement != null)
            {
                String classname = jsonElement.getAsString();
                List<String> params = new ArrayList<String>();
                if (paramObj != null)
                {
                    for (JsonElement param : paramObj)
                    {
                        params.add(param.toString());
                    }
                }
                Exception ex = customConnector.deserializeException(classname, message.toString(), params);
                try
                {
                    if (stacktrace != null)
                    {
                        List<StackTraceElement> trace = new ArrayList<StackTraceElement>();
                        for (JsonElement element : stacktrace.getAsJsonArray())
                        {
                            StackTraceElement ste = createJsonMapper().fromJson(element, StackTraceElement.class);
                            trace.add(ste);
                        }
                        ex.setStackTrace(trace.toArray(new StackTraceElement[] {}));
                    }
                }
                catch (Exception ex3)
                {
                    // Can't get stacktrace
                }
                return ex;
            }
        }
        return new RaplaConnectException(message.toString());
    }

    synchronized protected JsonElement sendCall_(String requestMethod, URL methodURL, JsonElement jsonObject,Map<String, String>additionalHeaders) throws Exception
    {
        String authenticationToken = customConnector.getAccessToken();
        return sendCall_(requestMethod, methodURL, jsonObject, authenticationToken, additionalHeaders);
    }

    synchronized protected JsonElement sendCall_(String requestMethod, URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws Exception
    {

        JsonElement resultMessage;
        try
        {
            resultMessage = sendCall(requestMethod, methodURL, jsonObject, authenticationToken, additionalHeaders);
        }
        catch (SocketException ex)
        {
            throw customConnector.getConnectError(ex);
        }
        catch (UnknownHostException ex)
        {
            throw customConnector.getConnectError(ex);
        }
        catch (FileNotFoundException ex)
        {
            throw customConnector.getConnectError(ex);
        }
        try
        {
            checkError(resultMessage);
        }
        catch (AuthenticationException ex)
        {
            String newAuthCode = customConnector.reauth(this);
            // try the same call again with the new result, this time with no auth code failed fallback
            if ( newAuthCode != null )
            {
                resultMessage = sendCall_("POST", methodURL, jsonObject, newAuthCode, additionalHeaders);
                checkError(resultMessage);
            }
            else
            {
                throw ex;
            }
        }
        return resultMessage;
    }


    protected void checkError(JsonElement resultMessage) throws Exception
    {
        if(resultMessage.isJsonObject())
        {
            JsonElement errorElement = resultMessage.getAsJsonObject().get("error");
            if (errorElement != null)
            {
                Exception ex = deserializeExceptionObject(resultMessage.getAsJsonObject());
                throw ex;
            }
            
        }
    }


    protected URL getMethodUrl(String classname, String subPath)
    {
        final String entryPoint = getServiceEntryPointFactory().getEntryPoint(classname, getPath())
                + (subPath == null || subPath.isEmpty() ? "" : (subPath.startsWith("?") ? "" : "/") + subPath);
        try
        {
            return new URL(entryPoint);
        }
        catch (MalformedURLException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected Object getResult(JsonElement resultElement, Class resultType, Class container) throws RaplaConnectException
    {
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

    public Method findMethod(Class<?> service, String methodName)
    {
        Method method = null;
        for (Method m : service.getMethods())
        {
            if (m.getName().equals(methodName))
            {
                method = m;
            }
        }
        if (method == null)
        {
            throw new IllegalStateException("Method " + methodName + " not found in " + service.getClass());
        }
        return method;
    }

    public JsonElement serializeCall(Object arg)
    {
//        Class<?>[] parameterTypes = method.getParameterTypes();
        JsonElement params = serializeArguments(arg);
        return params;
//        JsonObject element = new JsonObject();
//        element.addProperty("jsonrpc", "2.0");
//        element.addProperty("method", method.getName());
//        element.add("params", params);
//        element.addProperty("id", "1");
//        return element;
    }

    //    private void addParams(Appendable writer, Map<String,String> args ) throws IOException
    //    {
    //    	writer.append( "v="+URLEncoder.encode(clientVersion,"utf-8"));
    //        for (Iterator<String> it = args.keySet().iterator();it.hasNext();)
    //        {
    //        	writer.append( "&");
    //            String key = it.next();
    //            String value= args.get( key);
    //            {
    //                String pair = key;
    //                writer.append( pair);
    //                if ( value != null)
    //                {
    //                	writer.append("="+ URLEncoder.encode(value,"utf-8"));
    //                }
    //            }
    //
    //        }
    //    }

    public static class AuthenticationException extends Exception
    {
        public AuthenticationException(String message)
        {
            super(message);
        }
    }


}
