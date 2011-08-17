package com.mozilla.grouperfish.integration;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.annotations.Test;

import static com.mozilla.grouperfish.integration.IntegrationTestHelper.NS;


@Test(groups="integration")
@SuppressWarnings({ "unchecked", "serial" })
public class DocumentsResourceTest {

    final IntegrationTestHelper helper = new IntegrationTestHelper();

    private static final String DOC_A = (new JSONObject() {{
        put("id", "A");
        put("payload", "Whatever");
    }}).toJSONString();

    private static final String DOC_B = (new JSONObject() {{
        put("id", "B");
        put("subDoc", new JSONObject(){{
            put("myKey", "myValue");
        }});
    }}).toJSONString();

    private static final String DOC_C = (new JSONObject() {{
        put("id", "B");
        put("multivalueField", new JSONArray(){{
            add("one");
            add("two");
            add("three");
        }});
    }}).toJSONString();

    public void testPutDocument() {
        given().body(DOC_A).
            expect().statusCode(201).
            when().put(format("/documents/%s/A", NS));
        given().body(DOC_B).
            expect().statusCode(201).
            when().put(format("/documents/%s/B", NS));
        given().body(DOC_C).
            expect().statusCode(201).
            when().put(format("/documents/%s/C", NS));
    }

    public void testPutEmptyDocument() {
        given().body("{}").
            expect().statusCode(201).
            when().put(format("/documents/%s/X", NS));
    }

    public void testPutTooEmpty() {
        given().body("").
            expect().statusCode(400).
            when().put(format("/documents/%s/Z", NS));
    }

    public void testDeleteDocument() {
        testPutDocument();
        expect().
            statusCode(204).
            when().delete(format("/documents/%s/A", NS));
        expect().
            statusCode(204).
            when().delete(format("/documents/%s/B", NS));
        expect().
            statusCode(204).
            when().delete(format("/documents/%s/C", NS));
    }

    public void testRepeatDeleteDocument() {
        testPutDocument();
        expect().
            statusCode(204).
            when().delete(format("/documents/%s/A", NS));
        expect().
            statusCode(204).
            when().delete(format("/documents/%s/A", NS));
    }

    public void testGetDocument() {
        testPutDocument();
        expect().
            statusCode(200).
            when().get(format("/documents/%s/A", NS));
        expect().
            statusCode(200).
            when().get(format("/documents/%s/B", NS));
        expect().
            statusCode(200).
            when().get(format("/documents/%s/C", NS));
    }

    public void testNotFound() {
        expect().
            statusCode(404).
            when().get(format("/documents/%s/Yeti", NS));
    }

}
