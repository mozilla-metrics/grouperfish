package com.mozilla.grouperfish.json;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.model.Document;


public class Converters {

	public static JsonConverter<Document> forDocuments() {
        return new JsonConverter<Document>() {

            public String encode(final Document item) {
                return JSONObject.toJSONString(item.fields());
            }

            @SuppressWarnings("unchecked")
            public Document decode(final String json) {
            	Object fields;
                try {
                    fields = new JSONParser().parse(json);
                } catch (ParseException e) {
                    return Assert.unreachable(Document.class, e);
                }
            	Assert.check(fields instanceof JSONObject);
                return new Document((JSONObject) fields);
            }

        };
    }
}
