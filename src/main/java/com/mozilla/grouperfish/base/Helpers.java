package com.mozilla.grouperfish.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;

public class Helpers {

	/**
	 * http://stackoverflow.com/questions/1657193/
	 * @param input
	 * @return
	 */
	public static String toSlug(String input) {
	    String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
	    String normalized = Normalizer.normalize(nowhitespace, Form.NFD);
	    String slug = NONLATIN.matcher(normalized).replaceAll("");
	    return slug.toLowerCase(Locale.ENGLISH);
	}

	private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

	public static String consume(InputStream stream, Charset encoding) throws IOException {
	    final char[] buffer = new char[0x10000];
	    final StringBuilder out = new StringBuilder();
	    final Reader in = new InputStreamReader(stream, encoding);

	    int read;
	    do {
	        read = in.read(buffer, 0, buffer.length);
	        if (read>0) out.append(buffer, 0, read);
	    } while (read>=0);

	    return out.toString();
	}
}
