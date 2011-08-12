package com.mozilla.grouperfish.input;

import static org.testng.AssertJUnit.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.testng.annotations.Test;


@Test(groups="unit")
public class TsvReaderTest {

    static private final Charset UTF8 = Charset.forName("UTF-8");

    enum Fixture {
        SIMPLE(
                "",
                new String[][] {}),
        ONE_ROW(
                "three\tcolumn\trow",
                new String[][] { { "three", "column", "row" } }),
        TWO_ROWS(
                "one\\\tcolumn\\\trow",
                new String[][] { { "one\tcolumn\trow" } }),
        TRAILING_LF(
                "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n",
                new String[][] {{ "one\tcolumn\trow" }, { "two", "column\trow" } }),
        SOME(
                " many \n many",
                new String[][] { { " many " }, { " many" } }),
        MULTICHUNK(
                "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n"
                + "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n",
                new String[][] {
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" },
                        { "one\tcolumn\trow" }, { "two", "column\trow" }
                }),
        MANY(
                " many \n many \n many \n many \n lines",
                new String[][] {{ " many " }, { " many " }, { " many " }, { " many " },{ " lines" }});

        String in;
        String[][] out;

        Fixture(String i, String[][] o) {
            in = i;
            out = o;
        }
    }

    static void check(String source, String[][] expected) {
        final InputStream in = new ByteArrayInputStream(source.getBytes(UTF8));
        final TsvReader tsv = new TsvReader(in);
        List<String[]> list = new LinkedList<String[]>();
        do {
            try {
                String[] next = tsv.nextRow();
                if (next == null)
                    break;
                list.add(next);
            } catch (IOException e) {
                assert false;
            }
        } while (true);

        final String[][] actual = list.toArray(new String[][]{});
        assertEquals(expected.length, actual.length);

        for (int i = 0; i < expected.length; ++i) {
            final String[] eRow = expected[i];
            final String[] aRow = actual[i];
            assertEquals(eRow.length, aRow.length);
            for (int j = 0; j < eRow.length; ++j) {
                assertEquals(eRow[j], aRow[j]);
            }
        }

    }

    private void check(Fixture fixture) {
        check(fixture.in, fixture.out);
    }

    public void testSimple() {
        check(Fixture.SIMPLE);
    }

    public void testOneRow() {
        check(Fixture.ONE_ROW);
    }

    public void testTwoRows() {
        check(Fixture.TWO_ROWS);
    }

    public void testTrailingLf() {
        check(Fixture.TRAILING_LF);
    }

    public void testSome() {
        check(Fixture.SOME);
    }

    public void testMultichunk() {
        check(Fixture.MULTICHUNK);
    }

    public void testMany() {
        check(Fixture.MANY);
    }
}
