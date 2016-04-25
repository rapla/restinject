package org.rapla.rest.client.swing;

import org.rapla.rest.client.AuthenticationException;
import org.rapla.rest.client.CustomConnector;
import org.rapla.rest.client.ExceptionDeserializer;
import org.rapla.rest.client.RaplaConnectException;
import org.rapla.rest.client.SerializableExceptionInformation;
import org.rapla.rest.client.SerializableExceptionInformation.SerializableExceptionStacktraceInformation;

import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
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

    synchronized public Object send(CustomConnector customConnector, String requestMethod, String methodURL, String body,
            Map<String, String> additionalHeaders, JavaJsonSerializer serializer) throws Exception
    {
        JsonRemoteConnector remote = remoteConnector;

        String contentType =  "application/json";
        String authenticationToken = customConnector.getAccessToken();
        JsonRemoteConnector.CallResult resultMessage;
        try
        {
            resultMessage = remote.sendCallWithString(requestMethod, new URL(methodURL), body, authenticationToken, contentType,additionalHeaders);
        }
        catch (SocketException | UnknownHostException | FileNotFoundException | MalformedURLException ex)
        {
            // TODO: Workaround to allow internatianlization of internal connect errors
            SerializableExceptionInformation exInfo = new SerializableExceptionInformation(new RaplaConnectException(ex.getMessage()));
            throw customConnector.deserializeException(exInfo);
        }
        try
        {
            checkError(customConnector, resultMessage, serializer);
        }
        catch (AuthenticationException ex)
        {
            String newAuthCode = customConnector.reauth(this.getClass());
            // try the same call again with the new result, this time with no auth code failed fallback
            if (newAuthCode != null)
            {
                try
                {
                    resultMessage = remote.sendCallWithString(requestMethod, new URL(methodURL), body, authenticationToken, contentType,additionalHeaders);
                    checkError(customConnector, resultMessage, serializer);
                }
                catch (SocketException | UnknownHostException | FileNotFoundException | MalformedURLException ex2)
                {
                    // TODO: Workaround to allow internatianlization of internal connect errors
                    SerializableExceptionInformation exInfo = new SerializableExceptionInformation(new RaplaConnectException(ex2.getMessage()));
                    throw customConnector.deserializeException(exInfo);
                }
            }
            else
            {
                throw ex;
            }
        }
        final Object result = serializer.deserializeResult(resultMessage.getResult());
        return result;
    }

    protected void checkError(ExceptionDeserializer customConnector, JsonRemoteConnector.CallResult resultMessage, JavaJsonSerializer serializer) throws Exception
    {
        final int responseCode = resultMessage.getResponseCode();
        if (responseCode != Response.Status.OK.getStatusCode() && responseCode != Response.Status.NO_CONTENT.getStatusCode())
        {

            Exception ex = deserializeExceptionObject(customConnector, resultMessage, serializer);
            throw ex;
        }
    }

    private Exception deserializeExceptionObject(ExceptionDeserializer customConnector, JsonRemoteConnector.CallResult resultMessage, JavaJsonSerializer serializer)
    {

        try
        {
            SerializableExceptionInformation deserializedException = serializer.deserializeException(resultMessage.getResult());
            if (deserializedException == null)
            {
                return new RaplaConnectException(resultMessage.getResponseCode() + ":" + resultMessage.getResult());
            }
            final Exception ex = customConnector.deserializeException(deserializedException);
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
            // unexpected exception occured, so throw RaplaConnectException
            return new RaplaConnectException(e.getMessage());
        }
    }

}
