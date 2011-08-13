package com.mozilla.grouperfish.integration;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import groovyx.net.http.ContentType;

import com.hazelcast.core.Hazelcast;
import com.jayway.restassured.RestAssured;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.service.Grouperfish;


@Test(groups="integration")
public class IntegrationTestHelper {

    private final int integrationPort = Grouperfish.DEFAULT_PORT + 100;

    public static String NS = "_integrationTest_";

    private Thread grouperfish = new Thread() {
        @Override
        public void run() {
            try {
                Grouperfish.main(new String[]{"integration.xml", "" + integrationPort});
            }
            catch (InterruptedException interrupt) {
                Hazelcast.getMap("documents_integrationTest").destroy();
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                Assert.unreachable(null, e);
            }
        }
    };


    @BeforeGroups(groups="integration")
    void setUp() throws InterruptedException {
        System.setProperty("server.port", "" + integrationPort);
        grouperfish.start();
        // Give some time for Grouperfish + HazelCast to come up:
        Thread.sleep(7000);

        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = integrationPort;
        RestAssured.basePath = "";
        RestAssured.requestContentType(ContentType.JSON);
    }

    @BeforeTest(groups="integration")
    void setUpRest() {
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = integrationPort;
        RestAssured.basePath = "";
        RestAssured.requestContentType(ContentType.JSON);
    }


    @AfterGroups(groups="integration")
    void tearDown() throws InterruptedException {
        grouperfish.interrupt();
        Thread.sleep(2000);
    }

}
