package org.rapla.client.swing;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.server.Server;
import org.rapla.rest.client.swing.HTTPConnector;
import org.rapla.rest.client.swing.JsonRemoteConnector.CallResult;
import org.rapla.rest.client.HTTPJsonConnector;
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
        URL baseUrl = new URL("http://localhost:8052/rapla/");
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
            JsonElement resultBody = connector.sendGet(methodURL,  emptyAuthenticationToken, additionalHeaders);
            final JsonObject asJsonObject = resultBody.getAsJsonObject();
            final JsonElement parse = asJsonObject.get("result");
            String resultObject = parse.getAsString();
            //authenticationToken = resultObject.get("accessToken").getAsString();
            //String validity = resultObject.get("validUntil").getAsString();
            assertEquals("Hello admin",resultObject);
        }
    }

    public void testPatch() throws Exception
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
        URL baseUrl = new URL("http://localhost:8052/rapla/");
        String username = "christopher";
        HTTPJsonConnector connector = new HTTPJsonConnector();
        {
            URL methodURL =new URL(baseUrl,"user/" + username );
            //JsonObject callObj = new JsonObject();
            //callObj.addProperty("username", username);
            //callObj.addProperty("password", password);
            String email;
            JsonParser jsonParser = new JsonParser();
            {
                JsonElement resultBody = connector.sendGet(methodURL);
                final JsonObject asJsonObject = resultBody.getAsJsonObject().get("result").getAsJsonObject();
                String resultUsername = asJsonObject.get("name").getAsString();
                email = asJsonObject.get("email").getAsString();
                assertEquals(username, resultUsername);
                assertTrue( email != null && email.length() > 0);
            }
            String newUsername = "martin";
            final JsonObject patchObject = new JsonObject();
            patchObject.add("name", new JsonPrimitive(newUsername));
            {
                JsonElement resultBody = connector.sendPatch(methodURL, patchObject);
                final JsonObject asJsonObject = resultBody.getAsJsonObject().get("result").getAsJsonObject();
                String resultUsername = asJsonObject.get("name").getAsString();
                assertEquals(newUsername, resultUsername);
                String resutEmail = asJsonObject.get("email").getAsString();
                assertEquals(email, resutEmail);
            }
        }
    }


    public void testHtml() throws Exception
    {
        URL baseUrl = new URL("http://localhost:8052/rapla/");
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
            CallResult resultBody = connector.sendCallWithString("GET",methodURL, body, emptyAuthenticationToken,"text/html", additionalHeaders);
            assertEquals("Hello admin",resultBody.getResult());
        }

    }
   
}
