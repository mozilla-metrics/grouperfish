package com.mozilla.grouperfish.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import com.mozilla.grouperfish.model.Document;

public class TsvJsonWriter {

    private final Writer writer;

    private final JsonConverter<Document> docConverter;

    /** You can put in a buffered writer, but you need to {@link #flush()} manually. */
    public TsvJsonWriter(final BufferedWriter writer) {
        this.writer = writer;
        this.docConverter = Converters.forDocuments();
    }

    public void write(final String key, final String jsonValue) throws IOException {
        writer.write(key.replace("\t", "\\t").replace("\n", "\\n"));
        writer.write("\t");
        writer.write(jsonValue.replace("\n", ""));
        writer.write("\n");
    }

    public void write(final Document doc) throws IOException {
        write(doc.id(), docConverter.encode(doc));
    }

    public void flush() throws IOException {
        writer.flush();
    }

}
