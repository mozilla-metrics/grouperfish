package com.mozilla.grouperfish.unit;

import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.mozilla.grouperfish.bootstrap.Grouperfish;


@Test(groups="unit")
public class UnitTestHelper {

    private final int port = Grouperfish.DEFAULT_PORT + 10;

    @BeforeGroups(groups="unit")
    void setUp() throws Exception {
        System.setProperty("hazelcast.config", "config/hazelcast.xml");
        System.setProperty("server.port", String.valueOf(port));
    }

}
