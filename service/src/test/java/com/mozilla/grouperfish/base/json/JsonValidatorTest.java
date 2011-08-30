package com.mozilla.grouperfish.base.json;

import java.io.IOException;

import org.testng.annotations.Test;

import com.mozilla.grouperfish.base.json.JsonValidator;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;


@Test(groups="unit")
public class JsonValidatorTest {

    public void testInvalidDocument() throws IOException {
        assertFalse(new JsonValidator().isValid("Your mom is valit!!!!"));
        assertFalse(new JsonValidator().isValid("{{{}}"));
    }

    public void testTooEmptyDocument() throws IOException {
        assertFalse(new JsonValidator().isValid(""));
    }

    public void testValidDocument() throws IOException {
        assertTrue(new JsonValidator().isValid("{}"));
        assertTrue(new JsonValidator().isValid("{\"a\": 1}"));
        assertTrue(new JsonValidator().isValid("{\"a\": 1, \"b\": 2}"));
    }

}
