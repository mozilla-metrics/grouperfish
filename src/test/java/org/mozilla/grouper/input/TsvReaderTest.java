package org.mozilla.grouper.input;

import static org.testng.AssertJUnit.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.mozilla.grouperfish.input.TsvReader;
import org.testng.annotations.Test;


@Test(groups = "unit")
public class TsvReaderTest {

    static private final Charset UTF8 = Charset.forName("UTF-8");

    enum Setup {
        SIMPLE("", new String[][]{}),
        ONE_ROW("three\tcolumn\trow",
                new String[][] {{"three", "column", "row"}}),
        TWO_ROWS("one\\\tcolumn\\\trow",
                 new String[][]{{"one\tcolumn\trow"}}),
        TRAILING_LF("one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n",
                    new String[][]{{"one\tcolumn\trow"}, {"two", "column\trow"}}),
        SOME(" many \n many",
             new String[][]{{" many "}, {" many"}}),
        MULTICHUNK("one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n" +
                   "one\\\tcolumn\\\trow\ntwo\tcolumn\\\trow\n",
                   new String[][]{
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"},
                        {"one\tcolumn\trow"},  {"two", "column\trow"}
                   }),
        MANY(" many \n many \n many \n many \n lines",
             new String[][]{{" many "}, {" many "}, {" many "}, {" many "}, {" lines"}});

        String in;
        String[][] out;
        Setup(String i, String[][] o) { in = i; out = o; }
    }

    private void check(Setup setup) {
        final InputStream in = new ByteArrayInputStream(setup.in.getBytes(UTF8));
        final TsvReader tsv = new TsvReader(in);
        List<String[]> list = new LinkedList<String[]>();
        do {
            try {
                String[] next = tsv.nextRow();
                if (next == null) break;
                list.add(next);
            }
            catch (IOException e) { assert false; }
        } while (true);
        assertEquals(setup.out, list.toArray(setup.out));
    }

    public void testSimple() { check(Setup.SIMPLE); }
    public void testOneRow() { check(Setup.ONE_ROW); }
    public void testTwoRows() { check(Setup.TWO_ROWS); }
    public void testTrailingLf() { check(Setup.TRAILING_LF); }
    public void testSome() { check(Setup.SOME); }
    public void testMultichunk() { check(Setup.MULTICHUNK); }
    public void testMany() { check(Setup.MANY); }
}
