package com.mozilla.grouperfish.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;


public class StreamTool {

    public static Charset UTF8 = Charset.forName("UTF-8");

    /**
     * @param stream The character source.
     * @param encoding An encoding, e.g. #UTF8
     */
    public static String consume(final InputStream stream, final Charset encoding)
    throws IOException {
        return maybeConsume(stream, encoding, 0);
    }

    /**
     * Consume everything from this reader into a string.
     * Close the reader when done.
     */
    public static String consume(final Reader in)
    throws IOException {
        Assert.nonNull(in);
        return consume(in, 0);
    }

    /**
     * Consume everything up to limit from this reader into a string.
     * If the stream has more characters than the given limit.
     *
     * @param A reader, will be closed when done.
     * @param limit If limit is reached while consuming the stream,
     *              <tt>null</tt> is returned.
     *              Set to <tt>0</tt> for no limit.
     * @return The contents, or <tt>null</tt> if the limit was exceeded.
     */
    public static String consume(final Reader in, final int limit)
    throws IOException {
        Assert.nonNull(in);
        final char[] buffer = new char[8192];
        final StringBuilder out = new StringBuilder();
        int size = 0;

        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            size += read;
            if (limit != 0 && size > limit) {
                in.close();
                return null;
            }
            if (read>0) out.append(buffer, 0, read);
        } while (read>=0);

        in.close();
        return out.toString();
    }

    /**
     * @param stream The character source.
     * @param encoding An encoding, e.g. #UTF8
     * @param limit If limit is reached while consuming the stream,
     *              <tt>null</tt> is returned.
     *              Set to <tt>0</tt> for no limit.
     */
    public static String maybeConsume(final InputStream stream, final Charset encoding, final int limit)
    throws IOException {
        Assert.nonNull(stream, encoding);
        return consume(new InputStreamReader(stream, encoding), limit);
    }


}
