package com.mozilla.grouperfish.integration;

//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.hbase.HBaseConfiguration;
//import org.apache.hadoop.hbase.LocalHBaseCluster;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import groovyx.net.http.ContentType;

import com.mozilla.grouperfish.bootstrap.Grouperfish;

import com.hazelcast.core.Hazelcast;
import com.jayway.restassured.RestAssured;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.rest.jersey.JerseyGuiceRestService;


@Test(groups="integration")
public class IntegrationTestHelper {

    public static final int port = Grouperfish.DEFAULT_PORT + 100;
    static {
        setUpRestAssured();
    }

    public static String NS = "integration";

    // private LocalHBaseCluster hbase;

    private final Thread grouperfish = new Thread() {
        @Override
        public void run() {
            System.setProperty("hazelcast.config", "config/hazelcast.xml");
            System.setProperty(JerseyGuiceRestService.PROPERTY_PORT, String.valueOf(port));
            try {
                Grouperfish.main(new String[]{});
            }
            catch (InterruptedException interrupt) {
                Hazelcast.getMap("documents_" + NS).destroy();
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                Assert.unreachable(null, e);
            }
        }
    };


    @BeforeGroups(groups="integration")
    void setUp() throws Exception {

        // Local HBaseCluster to use.
        // hbase = new LocalHBaseCluster(HBaseConfiguration.create(new Configuration()));
        // hbase.startup();
        // Thread.sleep(3000);

        // Set required bagheera configuration:

        // Give some time for Grouperfish (and especially HazelCast) to come up:
        grouperfish.start();
        Thread.sleep(10000);

        setUpRestAssured();
    }


    @AfterGroups(groups="integration")
    void tearDown() throws InterruptedException {
        grouperfish.interrupt();
        Thread.sleep(2000);
        //hbase.shutdown();
        //hbase.join();
    }


    @BeforeTest(groups="integration")
    public static void setUpRestAssured() {
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.requestContentType(ContentType.JSON);
    }

}
