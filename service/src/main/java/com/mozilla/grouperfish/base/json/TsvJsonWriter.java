package com.mozilla.grouperfish.base.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import com.mozilla.grouperfish.model.Document;


/** If using a buffered writer, make sure to {@link #flush()} when you are done. */
public class TsvJsonWriter {

    private final Writer writer;

    public TsvJsonWriter(final Writer writer) {
        this.writer = new BufferedWriter(writer);
    }

    public void write(final String key, final String source) throws IOException {
        writer.write(key.replace("\t", "\\t").replace("\n", "\\n"));
        writer.write("\t");
        writer.write(source.replace("\n", ""));
        writer.write("\n");
    }

    public void write(final Document document) throws IOException {
        write(document.id(), document.source());
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }
}
