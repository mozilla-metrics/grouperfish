package com.mozilla.grouperfish.base.json;

import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JsonValidator {

    static Logger log = LoggerFactory.getLogger(JsonValidator.class);

    private final JsonFactory jsonFactory = new JsonFactory();

    public boolean isValid(String json) throws IOException {
        if (json.length() == 0) {
            return false;
        }

        try {
            JsonParser parser = jsonFactory.createJsonParser(json);
            while (parser.nextToken() != null) { }
        } catch (JsonParseException e) {
            log.error("Error parsing JSON", e);
            return false;
        }

        return true;
    }

}
