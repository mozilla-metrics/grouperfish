package com.mozilla.grouperfish.input;

import java.io.ByteArrayInputStream;

import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.model.Document;


@Test(groups = "unit")
public class OpinionStreamTest {

    /** These are actual opinions from Firefox Input. */
    enum Fixture {
        SINGLE(
                "1\t1277293943\tpraise\tfirefox\t3.6.4\tmac\ten-US\t\t\t\tThis new way to give feedback is fantastic.",
                new String[][]{
                        {"1", "1277293943", "praise", "firefox", "3.6.4", "mac", "en-US", "", "", "", "This new way to give feedback is fantastic."}
                }
        ),
        MULTI(
                "59\t1277844585\tissue\tfirefox\t4.0b1\tlinux\ten-US\t\t\t\tNew load icon is to \"heavy\" and the \"pulse\" to distracting, some pages don't load complete for a long time.\n"
                + "60\t1277844766\tissue\tfirefox\t4.0b1\tmac\ten-US\t\t\t\tHave to re-download flash.\n"
                + "61\t1277845095\tissue\tfirefox\t4.0b1\tmac\ten-US\t\t\t\t4.0 beta 1 candidate tabs on top implementation in Mac OSX is refined, need to be more akin to the original 4.0 mockup screens from the wiki\n"
                + "62\t1277845108\tissue\tfirefox\t4.0b1\tmac\ten-US\t\t\thttp://img293.imageshack.us/f/submitfeedbackfirefoxin.png/\tScreenshot   mac osx 10.5.8\n"
                + "63\t1277845154\tpraise\tfirefox\t4.0b1\tmac\ten-US\t\t\t\t4.0 Beta 1 Candidate for Mac OSX (64-bit version) is much faster to start up\n"
                + "64\t1277845281\tpraise\tfirefox\t4.0b1\tmac\ten-US\t\t\t\tLike the \"pie chart\" page load indicator\n"
                + "65\t1277845622\tpraise\tfirefox\t4.0b1\tmac\ten-US\t\t\t\tImprovements in the Mac's toolbar appearance.",
                new String[][] {
                        {"59", "1277844585", "issue", "firefox", "4.0b1", "linux", "en-US", "", "", "", "New load icon is to \"heavy\" and the \"pulse\" to distracting, some pages don't load complete for a long time."},
                        {"60", "1277844766", "issue", "firefox", "4.0b1", "mac", "en-US", "", "", "", "Have to re-download flash."},
                        {"61", "1277845095", "issue", "firefox", "4.0b1", "mac", "en-US", "", "", "", "4.0 beta 1 candidate tabs on top implementation in Mac OSX is refined, need to be more akin to the original 4.0 mockup screens from the wiki"},
                        {"62", "1277845108", "issue", "firefox", "4.0b1", "mac", "en-US", "", "", "http://img293.imageshack.us/f/submitfeedbackfirefoxin.png/", "Screenshot   mac osx 10.5.8"},
                        {"63", "1277845154", "praise", "firefox", "4.0b1", "mac", "en-US", "", "", "", "4.0 Beta 1 Candidate for Mac OSX (64-bit version) is much faster to start up"},
                        {"64", "1277845281", "praise", "firefox", "4.0b1", "mac", "en-US", "", "", "", "Like the \"pie chart\" page load indicator"},
                        {"65", "1277845622", "praise", "firefox", "4.0b1", "mac", "en-US", "", "", "", "Improvements in the Mac's toolbar appearance."},
                }
        ),
        TOO_SHORT(
                "1\t1277293943\tpraise\tfirefox\t3.6.4\tmac\ten-US\tThis new way to give feedback is fantastic.",
                new String[][]{
                        {"1", "1277293943", "praise", "firefox", "3.6.4", "mac", "en-US", "This new way to give feedback is fantastic."}
                }
        );

        String in;
        String[][] out;

        Fixture(String i, String[][] o) {
            in = i;
            out = o;
        }
    }

    private void checkReader(Fixture fixture) {
        // The TsvReader test covers most of our testing needs.
        TsvReaderTest.check(fixture.in, fixture.out);
    }

    private void checkDocuments(Fixture fixture) {
        final Iterable<Document> opinions =
            new OpinionStream(new ByteArrayInputStream(fixture.in.getBytes(StreamTool.UTF8)));
        int i = 0;
        for (final Document opinion : opinions) {
            assertNotNull(opinion);
            assertNotNull(opinion.id());
            assertNotNull(opinion.fields());
            assertEquals(opinion.fields().size(), fixture.out[0].length);
            ++i;
        }
        assertEquals(i, fixture.out.length);
    }


    public void testSingle() {
        checkReader(Fixture.SINGLE);
        checkDocuments(Fixture.SINGLE);
    }

    public void testMulti() {
        checkReader(Fixture.MULTI);
        checkDocuments(Fixture.MULTI);
    }

    public void testShort() {
        checkReader(Fixture.TOO_SHORT);
        final Iterable<Document> opinions =
            new OpinionStream(new ByteArrayInputStream(Fixture.TOO_SHORT.in.getBytes(StreamTool.UTF8)));
        for (final Document opinion : opinions) {
            // too short rows should be skipped
            fail(opinion.id());
        }
    }

}
