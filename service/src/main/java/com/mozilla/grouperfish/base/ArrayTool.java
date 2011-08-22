package com.mozilla.grouperfish.base;

public class ArrayTool {

    public static byte[] concat(final byte[] a, final byte[] b) {
       final byte[] c = new byte[a.length + b.length];
       System.arraycopy(a, 0, c, 0, a.length);
       System.arraycopy(b, 0, c, a.length, b.length);
       return c;
    }

}
