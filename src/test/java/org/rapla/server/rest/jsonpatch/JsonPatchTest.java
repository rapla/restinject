package org.rapla.server.rest.jsonpatch;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.rapla.rest.server.jsonpatch.JsonMergePatch;
import org.rapla.rest.server.jsonpatch.JsonPatchException;

public class JsonPatchTest extends TestCase {
    public void test() throws JsonPatchException
    {
        JsonParser parser = new JsonParser();
        String jsonOrig = new String("{'classification': {'type' :  'MyResourceTypeKey',   'data':   {'name' : ['New ResourceName'] } } }");
        String newName = "Room A1234";
        String jsonPatch = new String("{'classification': { 'data':   {'name' : ['"+newName+"'] } } }");
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

