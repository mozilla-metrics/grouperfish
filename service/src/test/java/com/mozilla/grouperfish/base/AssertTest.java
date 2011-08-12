package com.mozilla.grouperfish.base;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;


@Test(groups="unit")
public class AssertTest {

    public void testNonNullPass() {
        Assert.nonNull(new int[100]);
        Assert.nonNull("a", 123, new Object());
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testNonNullFailSingle() {
        final String nothing = null;
        Assert.nonNull(nothing);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testNonNullFailMulti() {
        Assert.nonNull("a", 123, new Object(), null);
    }

    public void testCheckPass() {
        Assert.check(true);
        Assert.check(true, true);
        Assert.check(true, true, true);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCheckFailSingle() {
        Assert.check(false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testCheckFailMulti() {
        Assert.check(true, true, false);
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testUnreachable() {
        Assert.unreachable();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testUnreachableType() {
        String bogus = Assert.unreachable(String.class);
        fail(bogus);
    }

    public void testUnreachableWrap() {
        Exception inner = new RuntimeException();

        try {
            String neverAssigned = Assert.unreachable(String.class, inner);
            fail(neverAssigned);
        }
        catch (IllegalStateException e) {
            assertEquals(inner, e.getCause());
        }
    }

    public void testUnreachableArgs() {
        try {
            Assert.unreachable("Arrrgh");
            fail();
        }
        catch (IllegalStateException e) {
            assertEquals(
                    "[ASSERTION FAILED] Code should be unreachable: Arrrgh\n",
                    e.getMessage());
        }

        try {
            Assert.unreachable("Wut: %s %s???", "Over", 9000);
            fail();
        }
        catch (IllegalStateException e) {
            assertEquals(
                    "[ASSERTION FAILED] Code should be unreachable: Wut: Over 9000???\n",
                    e.getMessage());
        }

        try {
            String neverAssigned =
                Assert.unreachable(String.class, "Wut: %s %s???", "Over", 9000);
            fail(neverAssigned);
        }
        catch (IllegalStateException e) {
            assertEquals(
                    "[ASSERTION FAILED] Code should be unreachable: Wut: Over 9000???\n",
                    e.getMessage());
        }
    }

}
