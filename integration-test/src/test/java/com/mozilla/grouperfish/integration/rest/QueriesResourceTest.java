package com.mozilla.grouperfish.integration.rest;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;

import org.json.simple.JSONObject;
import org.testng.annotations.Test;

import com.mozilla.grouperfish.integration.IntegrationTestHelper;


@Test(groups="integration")
@SuppressWarnings({ "unchecked", "serial" })
public class QueriesResourceTest {

    final IntegrationTestHelper helper = new IntegrationTestHelper();
    final String NS = IntegrationTestHelper.NS;

    private static final String QUERY_ALL = (new JSONObject() {{
        put("query", new JSONObject(){{
            put("match_all", new JSONObject());
        }});
    }}).toJSONString();

    public void testPutQuery() {
        given().body(QUERY_ALL).
            expect().statusCode(201).
            when().put(format("/queries/%s/ALL", NS));
    }

    public void testPutTooEmpty() {
        given().body("").
            expect().statusCode(400).
            when().put(format("/queries/%s/Z", NS));
    }

//    // These tests cannot work yet (we first need to verify queries using ES).
//    public void testPutInvalidQuery() {
//        ...
//    }
//
//    public void testPutEmptyQuery() {
//        given().body("{}").
//            expect().statusCode(400).
//            when().put(format("/queries/%s/MYBAD", NS));
//    }


    public void testDeleteQuery() {
        testPutQuery();
        expect().
            statusCode(204).
            when().delete(format("/queries/%s/ALL", NS));
    }

    public void testRepeatDeleteQuery() {
        testPutQuery();
        expect().
            statusCode(204).
            when().delete(format("/queries/%s/ALL", NS));
        expect().
            statusCode(204).
            when().delete(format("/queries/%s/ALL", NS));
    }

    public void testGetQuery() {
        testPutQuery();
        expect().
            statusCode(200).
            when().get(format("/queries/%s/ALL", NS));
    }

    public void testNotFound() {
        expect().
            statusCode(404).
            when().get(format("/queries/%s/Yeti", NS));
    }

}
