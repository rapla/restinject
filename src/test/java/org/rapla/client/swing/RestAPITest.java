package org.rapla.client.swing;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.rapla.rest.client.EntryPointFactory;
import org.rapla.rest.client.swing.BasicRaplaHTTPConnector;
import org.rapla.rest.client.swing.HTTPConnector;
import org.rapla.rest.client.swing.HTTPConnector.HttpCallResult;
import org.rapla.rest.client.swing.HTTPJsonConnector;
import org.rapla.server.ServletTestContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import junit.framework.TestCase;

public class RestAPITest extends  TestCase {
    Server server;

    public RestAPITest(String name) {
        super(name);
    }

    @Override protected void setUp() throws Exception
    {
        super.setUp();
        server = ServletTestContainer.createServer();
        server.start();
        BasicRaplaHTTPConnector.setServiceEntryPointFactory(new EntryPointFactory()
        {
            @Override public String getEntryPoint(String interfaceName, String relativePath)
            {
                return "http://localhost:8052/" + "rest/" + (relativePath != null ? relativePath : interfaceName);
            }
        });
    }

    @Override protected void tearDown() throws Exception
    {
        super.tearDown();
        server.stop();
    }

    public void testRestApi() throws Exception
    {
        RestAPIExample example = new RestAPIExample()
        {
            protected void assertTrue( boolean condition)
            {
                TestCase.assertTrue(condition);
            }
            
            protected void assertEquals( Object o1, Object o2)
            {
                TestCase.assertEquals(o1, o2);
            }
        };
        URL baseUrl = new URL("http://localhost:8052/rest/");
        String username = "admin";
        String password = "secret";
        HTTPJsonConnector connector = new HTTPJsonConnector();

        // first we login using the auth method
        String authenticationToken = null;
        {
            URL methodURL =new URL(baseUrl,"auth/" + username + "?password="+ password);
            //JsonObject callObj = new JsonObject();
            JsonElement callObj = new JsonPrimitive(password);
            //callObj.addProperty("username", username);
            //callObj.addProperty("password", password);
            String emptyAuthenticationToken = null;
            Map<String, String> additionalHeaders = new HashMap<>();
            HttpCallResult resultBody = connector.sendGet(methodURL,  emptyAuthenticationToken, additionalHeaders);
            String resultObject = resultBody.parseJson().getAsString();
            //authenticationToken = resultObject.get("accessToken").getAsString();
            //String validity = resultObject.get("validUntil").getAsString();
            assertEquals("Hello admin",resultObject);
        }
    }


    public void testHtml() throws Exception
    {
        URL baseUrl = new URL("http://localhost:8052/rest/");
        String username ="admin";
        String password= "secret";
        HTTPConnector connector = new HTTPConnector();

        // first we login using the auth method
        String authenticationToken = null;
        {
            URL methodURL =new URL(baseUrl,"auth/" + username + "?password="+ password);
            //JsonObject callObj = new JsonObject();
            JsonElement callObj = new JsonPrimitive(password);
            //callObj.addProperty("username", username);
            //callObj.addProperty("password", password);
            String emptyAuthenticationToken = null;
            String body = "";
            Map<String, String> additionalHeaders = new HashMap<>();
            HttpCallResult resultBody = connector.sendCallWithString("GET",methodURL, body, emptyAuthenticationToken,"text/html", additionalHeaders);
            assertEquals("Hello admin",resultBody.getResult());
        }

    }
   
}
