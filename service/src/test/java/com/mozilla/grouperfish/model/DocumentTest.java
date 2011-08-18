package com.mozilla.grouperfish.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;


@Test(groups="unit")
@SuppressWarnings("serial")
public class DocumentTest {

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testEmptyDocument() {
        final Map<String, Object> empty = Collections.emptyMap();
        new Document(empty).source();
    }

    public void testVerySimpleDocument() {
        final Map<String, Object> fields = new HashMap<String, Object>() {{
            put("id", 1323);
        }};
        Document doc = new Document(fields);
        assertEquals("{\"id\":1323}", doc.source());
        assertEquals("1323", doc.name());
        assertEquals("1323", doc.id());
    }

    public void testSimpleDocument() {
        final Map<String, Object> fields = new HashMap<String, Object>() {{
            put("id", 1323);
            put("something", "else");
        }};
        Document doc = new Document(fields);
        assertEquals("1323", doc.id());
        assertEquals("else", doc.fields().get("something"));
    }

}
