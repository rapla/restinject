package org.rapla.server.rest.jsonpatch;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.rapla.rest.JsonParserWrapper;
import org.rapla.rest.gson.JsonMergePatch;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonPatchTest extends TestCase {
    public void test()
    {
        String jsonOrig = new String("{'classification': {'type' :  'MyResourceTypeKey',   'data':   {'name' : ['New ResourceName'] } } }");
        String newName = "Room A1234";
        String jsonPatch = new String("{'classification': { 'data':   {'name' : ['"+newName+"'] } } }");
        JsonParserWrapper.JsonParser jsonParser = JsonParserWrapper.defaultJson().get();
        Allocatable orig = jsonParser.fromJson(new StringReader(jsonOrig), Allocatable.class);
        String patched = jsonParser.patch(orig, new StringReader(jsonPatch));
        System.out.println("Original " +jsonOrig);
        System.out.println("Patch    " +jsonPatch);
        System.out.println("Patched  " +patched);
        String jsonExpected = new String("{\"classification\":{\"type\":\"MyResourceTypeKey\",\"data\":{\"name\":[\""+ newName +"\"]}}}");
        assertEquals( jsonExpected, patched);
    }

    static class Allocatable {
        Classification classification;
    }
    static class Classification {
        String type;
        Map<String, List<String>> data = new LinkedHashMap<>();
    }

}

