package com.mozilla.grouperfish.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

//:TODO: Unit Test
public class StreamTool {

    /**
     * @param stream The character source.
     * @param encoding An encoding, e.g. #UTF8
     */
    public static String consume(InputStream stream, Charset encoding) throws IOException {
        return consume(stream, encoding, 0);
    }

    /**
     * @param stream The character source.
     * @param encoding An encoding, e.g. #UTF8
     * @param limit If limit is reached while consuming the stream, <tt>null</tt> is returned.
     *              Set to <tt>0</tt> for no limit.
     *
     * @throws IOException
     */
    public static String consume(InputStream stream, Charset encoding, int limit) throws IOException {
        final char[] buffer = new char[8192];
        final StringBuilder out = new StringBuilder();
        final Reader in = new InputStreamReader(stream, encoding);

        int size = 0;

        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            size += read;
            if (limit != 0 || size > limit) return null;
            if (read>0) out.append(buffer, 0, read);
        } while (read>=0);

        return out.toString();
    }

}
