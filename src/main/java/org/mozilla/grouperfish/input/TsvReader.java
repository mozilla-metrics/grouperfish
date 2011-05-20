package org.mozilla.grouperfish.input;

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
 */
public class TsvReader {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final int BUF_SIZE = 32768 * 32;

  private boolean escaped = false;
  private final StringBuilder builder = new StringBuilder();
  private final BufferedReader reader;


  public
  TsvReader(final InputStream in) {
    reader = new BufferedReader(new InputStreamReader(in, UTF8), BUF_SIZE);
  }


  public
  String[] nextRow() throws IOException {
    final List<String> row = new LinkedList<String>();
    char c;
    while (true) {
      int i = reader.read();
      if (i == -1) return null;
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
