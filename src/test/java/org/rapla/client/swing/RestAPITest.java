package org.rapla.client.swing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.rapla.jsonrpc.client.EntryPointFactory;
import org.rapla.jsonrpc.client.swing.BasicRaplaHTTPConnector;
import org.rapla.jsonrpc.client.swing.HTTPConnector;
import org.rapla.jsonrpc.client.swing.HTTPJsonConnector;
import org.rapla.server.ServletTestContainer;
import org.rapla.server.TestServlet;

import java.net.URL;

public class RestAPITest extends  TestCase {
    Server server;

    public RestAPITest(String name) {
        super(name);
    }

    @Override protected void setUp() throws Exception
    {
        super.setUp();
        server = ServletTestContainer.createServer(TestServlet.class);
        BasicRaplaHTTPConnector.setServiceEntryPointFactory(new EntryPointFactory()
        {
            @Override public String getEntryPoint(String interfaceName, String relativePath)
            {
                return "http://localhost:8052/" + "rapla/" + (relativePath != null ? relativePath : interfaceName);
            }
        });
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
            JsonObject resultBody = connector.sendGet(methodURL,  emptyAuthenticationToken);
            String resultObject = resultBody.get("result").getAsString();
            //authenticationToken = resultObject.get("accessToken").getAsString();
            //String validity = resultObject.get("validUntil").getAsString();
            assertEquals("Hello admin",resultObject);
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
            String resultBody = connector.sendCallWithString("GET",methodURL, body, emptyAuthenticationToken,"text/html");
            assertEquals("Hello admin",resultBody);
        }

    }
   
}
