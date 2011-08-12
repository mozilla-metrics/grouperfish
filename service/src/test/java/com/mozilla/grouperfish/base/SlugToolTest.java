package com.mozilla.grouperfish.base;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;


@Test(groups="unit")
public class SlugToolTest {

    public void testToSlug() {

        assertEquals("my-name-is-joe", SlugTool.toSlug("My Name is Joe"));
        assertEquals("wut-over-9000", SlugTool.toSlug("Wut, over 9000?!?"));
        assertEquals("space-----madness", SlugTool.toSlug("Space     Madness"));

    }

}
