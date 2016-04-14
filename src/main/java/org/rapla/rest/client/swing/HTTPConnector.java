package org.rapla.rest.client.swing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class HTTPConnector
{
    public HttpCallResult sendCallWithString(String requestMethod, URL methodURL, String body, String authenticationToken,
            Map<String, String> additionalHeaders) throws IOException, ProtocolException, UnsupportedEncodingException
    {
        return sendCallWithString(requestMethod, methodURL, body, authenticationToken, "application/json", additionalHeaders);
    }

    public HttpCallResult sendCallWithString(String requestMethod, URL methodURL, String body, String authenticationToken, String accept,
            Map<String, String> additionalHeaders) throws IOException, ProtocolException, UnsupportedEncodingException
    {
        HttpURLConnection conn = (HttpURLConnection) methodURL.openConnection();
        for (Entry<String, String> additionalHeader : additionalHeaders.entrySet())
        {
            conn.setRequestProperty(additionalHeader.getKey(), additionalHeader.getValue());
        }
        if (!requestMethod.equals("POST") && !requestMethod.equals("GET"))
        {
            conn.setRequestMethod("POST");
            // we tunnel all non POST or GET requests to avoid proxy filtering (e.g. URLConnection does not allow PATCH)
            conn.setRequestProperty("X-HTTP-Method-Override", requestMethod);
        }
        else
        {
            conn.setRequestMethod(requestMethod);
        }
        conn.setUseCaches(false);
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        conn.setRequestProperty("Content-Type", "application/json" + ";charset=utf-8");
        conn.setRequestProperty("Accept", accept);
        if (authenticationToken != null)
        {
            conn.setRequestProperty("Authorization", "Bearer " + authenticationToken);
        }
        conn.setReadTimeout(60000); //set timeout to 60 seconds
        conn.setConnectTimeout(50000); //set connect timeout to 50 seconds
        conn.setDoOutput(true);
        conn.connect();

        if (requestMethod.equals("PUT") || requestMethod.equals("POST") || requestMethod.equals("PATCH"))
        {
            OutputStream outputStream = null;
            Writer wr = null;
            try
            {
                outputStream = conn.getOutputStream();
                wr = new OutputStreamWriter(outputStream, "UTF-8");
                if (body != null)
                {
                    wr.write(body);
                    wr.flush();
                }
                else
                {
                    wr.flush();
                }
            }
            finally
            {
                if (wr != null)
                {
                    wr.close();
                }
                if (outputStream != null)
                {
                    outputStream.close();
                }
            }
        }
        else
        {

        }
        String resultString;
        final int responseCode = conn.getResponseCode();
        {
            InputStream inputStream = null;
            try
            {
                if (responseCode != 200)
                {
                    inputStream = conn.getErrorStream();
                }
                else
                {
                    String encoding = conn.getContentEncoding();
                    if (encoding != null && encoding.equalsIgnoreCase("gzip"))
                    {
                        inputStream = new GZIPInputStream(conn.getInputStream());
                    }
                    else if (encoding != null && encoding.equalsIgnoreCase("deflate"))
                    {
                        inputStream = new InflaterInputStream(conn.getInputStream(), new Inflater(true));
                    }
                    else
                    {
                        inputStream = conn.getInputStream();
                    }
                }
                resultString = readResultToString(inputStream);
            }
            finally
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            }
        }
        return new HttpCallResult(resultString, responseCode);
    }

    private String readResultToString(InputStream input) throws IOException
    {
        InputStreamReader in = new InputStreamReader(input, "UTF-8");
        char[] buf = new char[4096];
        StringBuffer buffer = new StringBuffer();
        while (true)
        {
            int len = in.read(buf);
            if (len == -1)
            {
                break;
            }
            buffer.append(buf, 0, len);
        }
        String result = buffer.toString();
        in.close();
        return result;
    }

    public static class HttpCallResult
    {

        private final String result;
        private final int responseCode;

        public HttpCallResult(String result, int responseCode)
        {
            this.result = result;
            this.responseCode = responseCode;
        }

        public JsonElement parseJson() throws JsonParseException
        {
            JsonParser jsonParser = new JsonParser();
            JsonElement parsed = jsonParser.parse(result);
            return parsed;
        }

        public String getResult()
        {
            return result;
        }

        public int getResponseCode()
        {
            return responseCode;
        }
    }

}
