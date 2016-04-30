package org.rapla.rest.client.swing;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public abstract class AbstractLocalJsonConnector implements JsonRemoteConnector
{
    @Override public CallResult sendCallWithString(String requestMethod, URL methodURL, String body, String authenticationToken, String contentType,
            Map<String, String> additionalHeaders) throws IOException
    {
        final String rawHttpRequest = createRawHttpRequest(requestMethod, methodURL, body, authenticationToken, contentType, additionalHeaders);
        final String rawResult = doSend(rawHttpRequest);
        return parseCallResult(rawResult);
    }

    protected CallResult parseCallResult(String rawResult)
    {
        /*
HTTP/1.1 200 OK
Date: Mon, 23 May 2005 22:38:34 GMT
Content-Type: text/html; charset=UTF-8
Content-Encoding: UTF-8
Content-Length: 138
Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)
ETag: "3f80f-1b6-3e1cb03b"
Accept-Ranges: bytes
Connection: close

<html>
<head>
  <title>An Example Page</title>
</head>
<body>
  Hello World, this is a very simple HTML document.
</body>
</html>
         */
        BufferedReader br = new BufferedReader(new StringReader(rawResult));
        String body = "";
        Integer responseCode = null;
        boolean header = true;
        String line;
        try
        {
            while ((line = br.readLine()) != null)
            {
                if(!header)
                {
                    body += line;
                }
                if(responseCode == null)
                {
                    final String[] split = line.split(" ");
                    responseCode = Integer.parseInt(split[1]);
                }
                if(line.isEmpty())
                {
                    header = false;
                }
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("IOException thrown in String: "+e.getMessage(), e);
        }
        if (responseCode == null)
        {
            responseCode = Response.Status.NO_CONTENT.getStatusCode();
        }
        CallResult result = new CallResult(body, responseCode);
        return result;
    }

    protected abstract String doSend(String rawHttpRequest);

    private String createRawHttpRequest(String requestMethod, URL methodURL, String body, String authenticationToken, String contentType,
            Map<String, String> additionalHeaders)
    {
        final StringWriter out = new StringWriter();
        PrintWriter buf = new PrintWriter(out);
        buf.print(requestMethod + " " + methodURL.toString() + " HTTP/1.1");
        buf.print("\r\n");
        buf.print("Host: localhost");
        buf.print("\r\n");
        buf.print("Content-Type: " + contentType + ";charset=utf-8");
        buf.print("\r\n");
        buf.print("Content-Length: " + body.getBytes(Charset.forName("UTF-8")).length);
        buf.print("\r\n");
        buf.print("Accept: " + contentType);
        buf.print("\r\n");
        for (Map.Entry<String, String> et : additionalHeaders.entrySet())
        {
            final String key = et.getKey();
            final String value = et.getValue();
            buf.print(key);
            buf.print(": ");
            buf.print(value);
            buf.print("\r\n");
        }
        if (authenticationToken != null)
        {
            buf.print("Authorization: Bearer " + authenticationToken);
            buf.print("\r\n");
        }
        buf.print("\r\n");
        buf.print(body);
        buf.print("\r\n\r\n");
        final String s = out.toString();
        return s;

    }

}
