package com.mozilla.grouperfish.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.json.simple.JSONValue;

import com.mozilla.grouperfish.base.StreamTool;


/**
 * Takes String keys and JSON values and streams them out as one JSON map,
 * without composing everything in memory.
 */
public class MapStreamer {

    private final Map<String, String> map;

    public MapStreamer(final Map<String, String> map) {
        this.map = map;
    }

    public void write(OutputStream out) throws IOException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter(out, StreamTool.UTF8));
        boolean first = true;

        writer.write('{');
        for (final Map.Entry<String, String> items : map.entrySet()) {
            if (first) {
                first = false;
            }
            else {
                writer.append(',');
                writer.append('\n');
            }

            writer.append('"');
            writer.append(JSONValue.escape(items.getKey()));
            writer.append('"');
            writer.append(':');
            writer.append(' ');
            writer.write(items.getValue());
        }
        writer.write('}');
        writer.flush();
    }

}

