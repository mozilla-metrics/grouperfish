package com.mozilla.grouperfish.integration;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.annotations.Test;


@Test(groups="integration")
@SuppressWarnings({ "unchecked", "serial" })
public class ConfigurationsResourceTest {

    final IntegrationTestHelper helper = new IntegrationTestHelper();
    final String NS = IntegrationTestHelper.NS;

    private static final String CONFIGURATION_KMEANS_STYLE = (new JSONObject() {{
        put("transform", "kmeans");
        put("parameters", new JSONObject() {{
            put("fields", new JSONArray() {{ add("description"); add("title"); }});
            put("k", 17);
        }});
    }}).toJSONString();

    private static final String FILTER_QUALITY_STYLE = (new JSONObject() {{
        put("filter", "QualityHeuristics");
        put("parameters", new JSONObject() {{
            put("minWords", 2);
        }});
    }}).toJSONString();

    public void testPutConfiguration() {
        given().body(CONFIGURATION_KMEANS_STYLE).
            expect().statusCode(201).
            when().put(format("/configurations/%s/transforms/themes", NS));
        given().body(FILTER_QUALITY_STYLE).
            expect().statusCode(201).
            when().put(format("/configurations/%s/filters/qulity", NS));
    }

    public void testPutTooEmpty() {
        given().body("").
            expect().statusCode(400).
            when().put(format("/configurations/%s/transforms/heregoesnothing", NS));
    }

    public void testPutUnknownType() {
        given().body(CONFIGURATION_KMEANS_STYLE).
            expect().statusCode(404).
            when().put(format("/configurations/%s/yetis_den/yeti_props", NS));
    }

//    // These tests cannot work yet (we first need to verify configurations using parameter schema).
//    public void testPutInvalidConfiguration() {
//        ...
//    }
//
//    public void testPutEmptyConfiguration() {
//        given().body("{}").
//            expect().statusCode(400).
//            when().put(format("/configurations/%s/transforms/MYBAD", NS));
//    }

    public void testDeleteConfiguration() {
        testPutConfiguration();
        expect().
            statusCode(204).
            when().delete(format("/configurations/%s/transforms/themes", NS));
        expect().
            statusCode(204).
            when().delete(format("/configurations/%s/filters/qulity", NS));
    }

    public void testRepeatDeleteConfiguration() {
        testPutConfiguration();
        expect().
            statusCode(204).
            when().delete(format("/configurations/%s/transforms/themes", NS));
        expect().
            statusCode(204).
            when().delete(format("/configurations/%s/transforms/themes", NS));
    }

    public void testGetConfiguration() {
        testPutConfiguration();
        expect().
            statusCode(200).
            when().get(format("/configurations/%s/transforms/themes", NS));
    }

    public void testNotFound() {
        expect().
            statusCode(404).
            when().get(format("/configurations/%s/transforms/SantaClaus", NS));
    }

}
