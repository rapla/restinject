package org.rapla.client.swing;

import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.rapla.jsonrpc.client.EntryPointFactory;
import org.rapla.jsonrpc.client.swing.BasicRaplaHTTPConnector;
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
        example.testRestApi(baseUrl,"admin","secret");
    }

   
}
