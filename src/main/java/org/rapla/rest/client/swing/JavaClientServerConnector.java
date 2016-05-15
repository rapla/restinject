package org.rapla.rest.client.swing;

import org.rapla.rest.client.AuthenticationException;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.ExceptionDeserializer;
import org.rapla.rest.client.RemoteConnectException;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.SerializableExceptionInformation.SerializableExceptionStacktraceInformation;
import org.rapla.rest.client.gwt.internal.impl.ResultDeserializer;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JavaClientServerConnector
{

    private static JsonRemoteConnector remoteConnector = new HTTPConnector();
    public JavaClientServerConnector()
    {
    }

    static public void setJsonRemoteConnector( JsonRemoteConnector remote)
    {
        remoteConnector = remote;
    }

    public static <T> T doInvoke(final String requestMethodType, final String url,  final Map<String, String> additionalHeaders,String body,
            final JavaJsonSerializer ser, String resultType, CustomConnector connector) throws Exception
    {
        final JavaClientServerConnector javaClientServerConnector = new JavaClientServerConnector();
        final T result = (T)javaClientServerConnector.send(requestMethodType, url, additionalHeaders, body, ser, resultType, connector);
        return result;
    }

    synchronized private Object send( String requestMethodType, String url, Map<String, String> additionalHeaders,String body,
             JavaJsonSerializer serializer,String resultType,CustomConnector customConnector) throws Exception
    {
        JsonRemoteConnector remote = remoteConnector;
        String contentType =  "application/json";
        String authenticationToken = customConnector.getAccessToken();
        JsonRemoteConnector.CallResult resultMessage;
        try
        {
            resultMessage = remote.sendCallWithString(requestMethodType, new URL(url), body, authenticationToken, contentType,additionalHeaders);
        }
        catch ( IOException ex)
        {
            // TODO: Workaround to allow internatianlization of internal connect errors
            SerializableExceptionInformation exInfo = new SerializableExceptionInformation(new RemoteConnectException(ex.getMessage()));
            int resultStatus = Response.Status.BAD_GATEWAY.getStatusCode();
            throw customConnector.deserializeException(exInfo,resultStatus);
        }
        Exception error = getError(customConnector, resultMessage, serializer);
        if ( error != null)
        {
            if (error instanceof AuthenticationException)
            {
                String newAuthCode = customConnector.reauth(this.getClass());
                // try the same call again with the new result, this time with no auth code failed fallback
                if (newAuthCode != null)
                {
                    try
                    {
                        resultMessage = remote.sendCallWithString(requestMethodType, new URL(url), body, authenticationToken, contentType, additionalHeaders);
                    }
                    catch (IOException ex)
                    {
                        // TODO: Workaround to allow internatianlization of internal connect errors
                        SerializableExceptionInformation exInfo = new SerializableExceptionInformation(new RemoteConnectException(ex.getMessage()));
                        int resultStatus = Response.Status.BAD_GATEWAY.getStatusCode();
                        throw customConnector.deserializeException(exInfo, resultStatus);
                    }
                }
                else
                {
                    throw error;
                }
            }
            else
            {
                throw error;
            }
        }
        final Object result = serializer.deserializeResult(resultMessage.getResult());
        return result;
    }

    protected Exception getError(ExceptionDeserializer customConnector, JsonRemoteConnector.CallResult resultMessage, JavaJsonSerializer serializer) throws Exception
    {
        final int responseCode = resultMessage.getResponseCode();
        if (responseCode != Response.Status.OK.getStatusCode() && responseCode != Response.Status.NO_CONTENT.getStatusCode())
        {

            Exception ex = deserializeExceptionObject(customConnector, resultMessage, serializer);
            return ex;
            //SerializableExceptionInformation exInfo = new SerializableExceptionInformation(new RemoteConnectException(ex.getMessage()));
            //return customConnector.deserializeException(exInfo, responseCode);
        }
        return null;
    }

    private Exception deserializeExceptionObject(ExceptionDeserializer customConnector, JsonRemoteConnector.CallResult resultMessage, JavaJsonSerializer serializer)
    {

        try
        {
            SerializableExceptionInformation deserializedException = serializer.deserializeException(resultMessage.getResult());
            if (deserializedException == null)
            {
                return new RemoteConnectException(resultMessage.getResponseCode() + ":" + resultMessage.getResult());
            }
            final Exception ex = customConnector.deserializeException(deserializedException,resultMessage.getResponseCode());
            try
            {
                final List<SerializableExceptionStacktraceInformation> stacktrace = deserializedException.getStacktrace();
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
        catch (Exception e)
        {
            // unexpected exception occured, so throw RemoteConnectException
            return new RemoteConnectException(e.getMessage());
        }
    }

}
