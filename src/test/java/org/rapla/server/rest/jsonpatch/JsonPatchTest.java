package org.rapla.server.rest.jsonpatch;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.rapla.rest.gson.JsonMergePatch;

public class JsonPatchTest extends TestCase {
    public void test()
    {
        String jsonOrig = new String("{'classification': {'type' :  'MyResourceTypeKey',   'data':   {'name' : ['New ResourceName'] } } }");
        String newName = "Room A1234";
        String jsonPatch = new String("{'classification': { 'data':   {'name' : ['"+newName+"'] } } }");
        JsonParser parser = new JsonParser();
        JsonElement patchElement = parser.parse(jsonPatch);
        JsonElement orig = parser.parse(jsonOrig);
        final JsonMergePatch patch = JsonMergePatch.fromJson(patchElement);
        final JsonElement patched = patch.apply(orig);
        System.out.println("Original " +orig.toString());
        System.out.println("Patch    " +patchElement.toString());
        System.out.println("Patched  " +patched.toString());
        String jsonExpected = new String("{'classification': {'type' :  'MyResourceTypeKey',   'data':   {'name' : ['"+ newName +"'] } } }");
        JsonElement expected = parser.parse(jsonExpected);
        assertEquals( expected.toString(), patched.toString());
    }

}

