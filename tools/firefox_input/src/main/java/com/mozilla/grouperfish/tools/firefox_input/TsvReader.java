package com.mozilla.grouperfish.tools.firefox_input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;


/**
 * TSV reading state machine. The opencsv lib does not support the
 * input.mozilla.com export format (escape without quotes).
 *
 * More information: https://wiki.mozilla.org/Firefox/Input/Data
 */
public class TsvReader {

    private static final int EOF = -1;
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final int BUF_SIZE = 32768 * 32;

	private boolean escaped = false;
	private boolean done = false;
	private final StringBuilder builder = new StringBuilder();
	private final BufferedReader reader;

	public TsvReader(final InputStream in) {
		reader = new BufferedReader(new InputStreamReader(in, UTF8), BUF_SIZE);
	}

	public String[] nextRow() throws IOException {
		final List<String> row = new LinkedList<String>();
		char c;
		while (true) {
		    if (done) {
		        return null;
		    }
			int i = reader.read();
            if (i == EOF) {
                done = true;
                if (builder.length() == 0)
                    return null;
                row.add(builder.toString());
                return row.toArray(new String[row.size()]);
            }

			c = (char) i;
			if (!escaped) {
				switch (c) {
				case '\\':
					escaped = true;
					continue;
				case '\t':
					row.add(builder.toString());
					builder.setLength(0);
					continue;
				case '\n':
					row.add(builder.toString());
					builder.setLength(0);
					return row.toArray(new String[row.size()]);
				}
			}
			builder.append(c);
			escaped = false;
		}
	}
}
