package com.mozilla.grouperfish.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;


@Test(groups="unit")
public class StreamToolTest {

    Charset UTF8 = Charset.forName("UTF-8");

    private String[] fixtures() {
        final String empty = "";
        final String single = "A";
        final String shortish =
            "The Mozilla project is a global community of people who believe that openness, "
            + "innovation, and opportunity are key to the continued health of the Internet. "
            + "We have worked together since 1998 to ensure that the Internet is developed "
            + "in a way that benefits everyone. As a result of the community's efforts, we "
            + "have distilled a set of principles that we believe are critical for the "
            + "Internet to continue to benefit the public good. These principles are "
            + "contained in the Mozilla Manifesto.";

        final String longish = shortish + shortish + shortish + shortish + shortish;
        final String longer = longish + longish +  longish + longish + longish;
        final String reallyLong = longer + longer + longer + longer + longer;

        final String unicode = "Internet se stává důležitou součástí našich životů.";

        return new String[]{empty, single, shortish,
                            longish, longer, reallyLong, unicode};
    }

    public void testConsumeInputStreamCharset() {
        for (String fixture : fixtures()) {
            InputStream stream = new ByteArrayInputStream(fixture.getBytes(UTF8));
            try {
                assertEquals(fixture, StreamTool.consume(stream, UTF8));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    public void testConsumeInputStreamCharsetLimit() {
        for (String fixture : fixtures()) {
            try {
                InputStream stream = new ByteArrayInputStream(fixture.getBytes(UTF8));
                assertEquals(fixture, StreamTool.maybeConsume(stream, UTF8, fixture.length()));

                if (fixture.length() <= 1) continue;
                stream = new ByteArrayInputStream(fixture.getBytes(UTF8));
                assertEquals(null, StreamTool.maybeConsume(stream, UTF8, fixture.length() - 1));
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMissingStream() throws IOException {
        StreamTool.maybeConsume(null, UTF8, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMissingEncoding() throws IOException {
        StreamTool.consume(new ByteArrayInputStream("lolwut".getBytes(UTF8)), null);
    }
}
