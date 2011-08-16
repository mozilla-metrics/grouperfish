package com.mozilla.grouperfish.json;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONValue;
import org.testng.annotations.Test;

import com.mozilla.grouperfish.model.Document;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;


@Test(groups="unit")
@SuppressWarnings("serial")
public class ConvertersTest {

    public void testEncodeSimpleDocument() {
        JsonConverter<Document> converter = Converters.forDocuments();
        final Map<String, Object> simple = new HashMap<String, Object>() {{
            put("id", "12345");
        }};
        assertEquals("{\"id\":\"12345\"}", converter.encode(new Document(simple)));
    }

    public void testEncodeDocument() {
        JsonConverter<Document> converter = Converters.forDocuments();
        final Map<String, Object> fields = new TreeMap<String, Object>() {{
            put("A", 1234);
            put("id", "XYZ");
        }};
        assertEquals(
                "{\"A\":1234,\"id\":\"XYZ\"}",
                converter.encode(new Document(fields)));

        final Map<String, Object> fields2 = new TreeMap<String, Object>() {{
            put("A", 1234);
            put("B", JSONValue.parse("{\"x\": 123}"));
            put("C", JSONValue.parse("{\"y\": [45, 67]}"));
            put("D", JSONValue.parse("{\"z\": 89}"));
            put("id", "XYZ");
        }};
        assertEquals(
                "{\"A\":1234,\"B\":{\"x\":123},\"C\":{\"y\":[45,67]},\"D\":{\"z\":89},\"id\":\"XYZ\"}",
                converter.encode(new Document(fields2)));
    }

    public void testDecodeDocument() {
        JsonConverter<Document> converter = Converters.forDocuments();
        Document d = converter.decode("{\"id\":\"12345\"}");
        assertTrue(d.fields().containsKey("id"));
    }

}
