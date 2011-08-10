package com.mozilla.grouperfish.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.json.simple.JSONValue;

import com.sun.jersey.json.impl.writer.JsonEncoder;


// :TODO: Unit Test
public class MapStreamer implements StreamingOutput {

    private final Map<String, String> map;

    public MapStreamer(final Map<String, String> map) {
        this.map = map;
    }

    public void write(OutputStream out) throws IOException, WebApplicationException {
        final Writer writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.write('{');
        boolean first = true;
        for (final Map.Entry<String, String> items : map.entrySet()) {
            if (first) {
                first = false;
            }
            else {
                writer.append(',');
                writer.append('\n');
            }

            JSONValue.escape(items.getKey());
            writer.write(JsonEncoder.encode(items.getValue()));
            writer.append(':');
        }
        writer.write('}');
    }
}

