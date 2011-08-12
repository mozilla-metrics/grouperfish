package com.mozilla.grouperfish.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.mozilla.grouperfish.json.Converters;
import com.mozilla.grouperfish.json.JSONConverter;

import static org.testng.AssertJUnit.assertEquals;


@Test(groups = "unit")
@SuppressWarnings("serial")
public class DocumentTest {

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testEmptyDocument() {
        JSONConverter<Document> converter = Converters.forDocuments();
        final Map<String, Object> empty = Collections.emptyMap();
        converter.encode(new Document(empty));
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
