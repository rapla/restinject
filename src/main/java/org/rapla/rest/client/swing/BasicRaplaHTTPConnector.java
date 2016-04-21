package org.rapla.rest.client.swing;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.rapla.rest.client.AuthenticationException;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.EntryPointFactory;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.SerializableExceptionInformation.SerializableExceptionStacktraceInformation;
import org.rapla.rest.client.gwt.MockProxy;

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

    final protected CustomConnector customConnector;

    protected String getMockAccessToken()
    {
        return customConnector.getAccessToken();
    }
    final GsonBuilder gsonBuilder;
    public BasicRaplaHTTPConnector(CustomConnector customConnector)
    {
        this.customConnector = customConnector;
        gsonBuilder = JSONParserWrapper.defaultGsonBuilder(customConnector.getNonPrimitiveClasses()).disableHtmlEscaping();
    }

    protected String serializeArgument(Object arg)
    {
        final String result;
        if(arg != null)
        {
            Gson gson = gsonBuilder.disableHtmlEscaping().create();
            result = gson.toJson( arg);
        }
        else
        {
            result = "";
        }
        return result;
    }

    protected String serializeArgumentUrl(Object arg)
    {
        final String result = serializeArgument( arg);
        try
        {
            return URLEncoder.encode( result,"UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return result;
        }
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

    private Exception deserializeExceptionObject(HttpCallResult resultMessage)
    {

        JsonElement errorElement = resultMessage.parseJson();
        if (errorElement == null || !errorElement.isJsonObject())
        {
            return new RaplaConnectException(resultMessage.getResponseCode() + ":" + resultMessage.getResult());
        }

        try
        {
            final Gson jsonMapper = createJsonMapper();
            SerializableExceptionInformation deserializedException = jsonMapper.fromJson(errorElement, SerializableExceptionInformation.class);
            final Exception ex = customConnector.deserializeException(deserializedException);
            try
            {
                final ArrayList<SerializableExceptionStacktraceInformation> stacktrace = deserializedException.getStacktrace();
                if (stacktrace != null)
                {
                    List<StackTraceElement> trace = new ArrayList<StackTraceElement>();
                    for (SerializableExceptionStacktraceInformation element : stacktrace)
                    {
                        final StackTraceElement ste = new StackTraceElement(element.getClassName(), element.getMethodName(), element.getFileName(),
                                element.getLineNumber());
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
        catch(Exception e)
        {
            // unexpected exception occured, so throw RaplaConnectException
            return new RaplaConnectException(e.getMessage());
        }
    }

    synchronized protected HttpCallResult sendCall_(String requestMethod, URL methodURL, JsonElement jsonObject,Map<String, String>additionalHeaders) throws Exception
    {
        String authenticationToken = customConnector.getAccessToken();
        return sendCall_(requestMethod, methodURL, jsonObject, authenticationToken, additionalHeaders);
    }



    synchronized protected HttpCallResult sendCallWithString_(String requestMethod, URL methodURL, String body, String authenticationToken,Map<String, String>additionalHeaders) throws Exception
    {
        HttpCallResult resultMessage;
        try
        {
            resultMessage = sendCallWithString(requestMethod, methodURL, body, authenticationToken, additionalHeaders);
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
            String newAuthCode = customConnector.reauth(this.getClass());
            // try the same call again with the new result, this time with no auth code failed fallback
            if ( newAuthCode != null )
            {
                resultMessage = sendCallWithString_("POST", methodURL, body, newAuthCode, additionalHeaders);
                checkError(resultMessage);
            }
            else
            {
                throw ex;
            }
        }
        return resultMessage;
    }


    synchronized protected HttpCallResult sendCall_(String requestMethod, URL methodURL, JsonElement jsonObject, String authenticationToken,Map<String, String>additionalHeaders) throws Exception
    {

        HttpCallResult resultMessage;
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
            String newAuthCode = customConnector.reauth(this.getClass());
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


    protected void checkError(HttpCallResult resultMessage) throws Exception
    {
        if(resultMessage.getResponseCode() != Response.Status.OK.getStatusCode())
        {

            Exception ex = deserializeExceptionObject(resultMessage);
            throw ex;
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

    protected Object getResult(HttpCallResult result, Class resultType, Class container) throws RaplaConnectException
    {
        final JsonElement resultElement = result.parseJson();
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

//
//    public Method findMethod(Class<?> service, String methodName)
//    {
//        Method method = null;
//        for (Method m : service.getMethods())
//        {
//            if (m.getName().equals(methodName))
//            {
//                method = m;
//            }
//        }
//        if (method == null)
//        {
//            throw new IllegalStateException("Method " + methodName + " not found in " + service.getClass());
//        }
//        return method;
//    }


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

}
