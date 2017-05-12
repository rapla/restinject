package org.rapla.rest.client.swing;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ChunkDecoderTest
{
    @Test
    public void testChunk()
    {
        String header = "HTTP/1.1 200 OK" + "\n" + "Transfer-Encoding: chunked" + "\n"+"\n";

        String content = "2\r\n" + "Ra\r\n" + "3\r\n" + "pla\r\n" + "E\r\n" + " in\r\n" + "\r\n" + "chunks.\r\n" + "0\r\n\r\n"
                + "\r\n";
        String complete = header + content;
        final JsonRemoteConnector.CallResult callResult = AbstractLocalJsonConnector.parseCallResult(complete);
        final String result = callResult.getResult();
        Assert.assertEquals("Rapla in\n\nchunks.", result);
    }
}
