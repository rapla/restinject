package org.rapla.rest.client;

import com.google.gson.*;
import org.rapla.gwtjsonrpc.client.impl.EntryPointFactory;
import org.rapla.gwtjsonrpc.common.AsyncCallback;
import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.JSONParserWrapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

public class BasicRaplaHTTPConnector extends HTTPJsonConnector
{
    private static EntryPointFactory serviceEntryPointFactory;
    //private String clientVersion;
    //CommandScheduler scheduler;
    String path;

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

    CustomConnector customConnector;
    protected Executor scheduler;

    public BasicRaplaHTTPConnector(CustomConnector customConnector)
    {
        this.scheduler = customConnector.getScheduler();
        this.customConnector = customConnector;
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

    private JsonArray serializeArguments(Class<?>[] parameterTypes, Object[] args)
    {
        final GsonBuilder gb = JSONParserWrapper.defaultGsonBuilder(customConnector.getNonPrimitiveClasses()).disableHtmlEscaping();
        JsonArray params = new JsonArray();
        Gson serializer = gb.disableHtmlEscaping().create();
        for (int i = 0; i < parameterTypes.length; i++)
        {
            Class<?> type = parameterTypes[i];
            Object arg = args[i];
            JsonElement jsonTree = serializer.toJsonTree(arg, type);
            params.add(jsonTree);
        }
        return params;
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

    public interface CustomConnector
    {
        String reauth(BasicRaplaHTTPConnector proxy) throws Exception;
        Exception deserializeException(String classname, String s, List<String> params);
        Class[] getNonPrimitiveClasses();
        Exception getConnectError(IOException ex);
        Executor getScheduler();
        String getAccessToken();
    }

    private Gson createJsonMapper()
    {
        Gson gson = JSONParserWrapper.defaultGsonBuilder(customConnector.getNonPrimitiveClasses()).disableHtmlEscaping().create();
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

    synchronized protected JsonObject sendCall_(String requestMethod, URL methodURL, JsonElement jsonObject) throws Exception
    {
        String authenticationToken = customConnector.getAccessToken();
        return sendCall_(requestMethod, methodURL, jsonObject, authenticationToken);
    }

    synchronized protected JsonObject sendCall_(String requestMethod, URL methodURL, JsonElement jsonObject, String authenticationToken) throws Exception
    {

        JsonObject resultMessage;
        try
        {
            resultMessage = sendCall(requestMethod, methodURL, jsonObject, authenticationToken);
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
                resultMessage = sendCall_("POST", methodURL, jsonObject, newAuthCode);
                checkError(resultMessage);
            }
            else
            {
                throw ex;
            }
        }
        return resultMessage;
    }


    protected void checkError(JsonObject resultMessage) throws Exception
    {
        JsonElement errorElement = resultMessage.get("error");
        if (errorElement != null)
        {
            Exception ex = deserializeExceptionObject(resultMessage);
            throw ex;
        }
    }


    protected URL getMethodUrl(String classname, String methodName)
    {
        final String entryPoint = getServiceEntryPointFactory().getEntryPoint(classname, getPath());
        try
        {
            return new URL(entryPoint);
        }
        catch (MalformedURLException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected Object getResult(JsonObject resultMessage, Class resultType, Class container) throws RaplaConnectException
    {
        JsonElement resultElement = resultMessage.get("result");
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

    public JsonObject serializeCall(Method method, Object[] args)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        JsonElement params = serializeArguments(parameterTypes, args);
        JsonObject element = new JsonObject();
        element.addProperty("jsonrpc", "2.0");
        element.addProperty("method", method.getName());
        element.add("params", params);
        element.addProperty("id", "1");
        return element;
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