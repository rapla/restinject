package org.rapla.client.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.rapla.jsonrpc.client.swing.HTTPJsonConnector;

import java.net.URL;

public class RestAPIExample {

    protected void assertTrue( boolean condition)
    {
        if (!condition)
        {
            throw new IllegalStateException("Assertion failed");
        }
    }
    
    protected void assertEquals( Object o1, Object o2)
    {
        if ( !o1.equals( o2))
        {
            throw new IllegalStateException("Assertion failed. Expected " + o1 + " but was " + o2);
        }
    }
    
    public void testRestApi(URL baseUrl, String username,String password) throws Exception
    {

    }

    public void assertNoError(JsonObject resultBody) {
        JsonElement error = resultBody.get("error");
        if (error!= null)
        {
            System.err.println(error);
            assertTrue( error == null );
        }
    }
    
    public static void main(String[] args) {
        try {
            // The base url points to the rapla servlet not the webcontext.
            // If your rapla context is not running under root webappcontext you need to add the context path.
            // Example if you deploy the rapla.war in tomcat the default would be
            // http://host:8051/rapla/rapla/
            URL baseUrl = new URL("http://localhost:8051/rapla/");
            String username = "admin";
            String password = "";
            new RestAPIExample().testRestApi(baseUrl, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
   
}
