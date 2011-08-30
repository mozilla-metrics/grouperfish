package com.mozilla.grouperfish.integration.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.RestAssured;
import com.mozilla.grouperfish.integration.IntegrationTestHelper;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.util.loader.DocumentLoader;
import com.mozilla.grouperfish.util.loader.Loader;

import static java.lang.String.format;
import static org.testng.AssertJUnit.assertEquals;
import static com.jayway.restassured.RestAssured.expect;
import static com.mozilla.grouperfish.base.ImmutableTools.put;

import static com.mozilla.grouperfish.integration.IntegrationTestHelper.NS;


@Test(groups="integration")
public class DocumentLoaderTest {

    @SuppressWarnings({"serial"})
    private static final Map<String, String> template = ImmutableMap.copyOf(new HashMap<String, String>() {{
        put("description", "Whatever");
        put("text", "This is the kind of text you'd expect in a document like this.");
        put("title", "Ph.D.");
    }});

    private String baseUrl;
    private Loader<Document> loader;

    @BeforeTest(groups="integration")
    public void setUp() {
        IntegrationTestHelper.setUpRestAssured();
        baseUrl = RestAssured.baseURI + ':' + RestAssured.port;
        loader = new DocumentLoader(baseUrl, NS);
    }

    public void testLoadSingleDoc() {
        loader.load(new Document(put(template, "id", "A_")));
    }

    public void testLoadSingleDocPlusVerify() {
        loader.load(new Document(put(template, "id", "AP_")));
        expect().
            statusCode(200).
            when().get(format("/documents/%s/%s", NS, "AP_"));
    }

    public void testLoadDuplicateDoc() {
        loader.load(new Document(put(template, "id", "B_")));
        loader.load(new Document(put(template, "id", "B_")));
    }

    public void testLoadSingleDocBatch() {
        final List<Document> batch = new ArrayList<Document>();
        batch.add(new Document(put(template, "id", "C_")));
        assertEquals(1, loader.load(batch));
    }

    public void testLoadSomeDocsBatch() {
        final List<Document> batch = new ArrayList<Document>();
        final int n = 10;
        for (int i = 0; i < n; ++i) {
            batch.add(new Document(put(template, "id", "D_" + i)));
        }
        assertEquals(n, loader.load(batch));
    }

    public void testLoadManyDocsBatch() {
        final List<Document> batch = new ArrayList<Document>();
        final int n = 1000;
        for (int i = 0; i < n; ++i) {
            batch.add(new Document(put(template, "id", "E_" + i)));
        }
        assertEquals(n, loader.load(batch));
    }

    public void testPlusVerify() {
        final List<Document> batch = new ArrayList<Document>();
        final int n = 20;
        for (int i = 0; i < n; ++i) {
            Document doc = new Document(put(template, "id", "F_" + i));
            batch.add(doc);
        }

        assertEquals(n, loader.load(batch));

        for (int i = 0; i < n; ++i) {
            expect().
                statusCode(200).
                when().get(format("/documents/%s/%s", NS, "F_" + i));
        }
    }

}
