package com.mozilla.grouperfish.base.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.testng.annotations.Test;

import com.mozilla.grouperfish.base.json.MapStreamer;

import static org.testng.AssertJUnit.assertEquals;


@Test(groups="unit")
public class MapStreamerTest {

    @SuppressWarnings("serial")
    enum Fixture {
        EMPTY(
                new HashMap<String, String>(),
                "{}"),
        ONE_ENTRY(
                new HashMap<String, String>() {{
                    put("item", "{\"something\": 123}");
                }},
                "{\"item\": {\"something\": 123}}"),
        MULTIPLE(
                new TreeMap<String, String>() {{
                    put("A", "{\"x\": 123}");
                    put("B", "{\"y\": [45, 67]}");
                    put("C", "{\"z\": 89}");
                }},
                "{\"A\": {\"x\": 123},\n\"B\": {\"y\": [45, 67]},\n\"C\": {\"z\": 89}}");


        Map<String, String> in;
        String expected;

        Fixture(Map<String, String> in, String out) {
            this.in = in;
            this.expected = out;
        }
    }

    private void check(Map<String, String> in, String expected) throws IOException {
        MapStreamer streamer = new MapStreamer(in);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamer.write(out);
        assertEquals(expected, out.toString("UTF-8"));
    }

    public void testEmpty() throws IOException {
        check(Fixture.EMPTY.in, Fixture.EMPTY.expected);
    }

    public void testOneEntry() throws IOException {
        check(Fixture.ONE_ENTRY.in, Fixture.ONE_ENTRY.expected);
    }

    public void testMultiple() throws IOException {
        check(Fixture.MULTIPLE.in, Fixture.MULTIPLE.expected);
    }

}
