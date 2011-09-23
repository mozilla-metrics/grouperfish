package com.mozilla.grouperfish.base.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.mozilla.grouperfish.base.StreamTool;


/**
 * Takes String keys and JSON values and streams them out as one JSON map,
 * without composing everything in memory.
 */
public class MapStreamer {

    private final Map<String, String> map;

    private static final ObjectMapper mapper = new ObjectMapper();

    public MapStreamer(final Map<String, String> map) {
        this.map = map;
    }

    public void write(OutputStream out) throws IOException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter(out, StreamTool.UTF8));
        boolean first = true;

        writer.write('{');
        for (final Map.Entry<String, String> item : map.entrySet()) {
            if (first) {
                first = false;
            }
            else {
                writer.append(',');
                writer.append('\n');
            }

            writer
                .append(mapper.writeValueAsString(item.getKey()))
                .append(':')
                .append(' ')
                .write(item.getValue());
        }
        writer.write('}');
        writer.flush();
    }

}

