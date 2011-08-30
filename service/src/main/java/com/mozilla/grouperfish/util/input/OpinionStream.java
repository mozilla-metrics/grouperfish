package com.mozilla.grouperfish.util.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.model.Document;


public class OpinionStream implements Iterable<Document> {

    public OpinionStream(final InputStream in) {
        in_ = in;
    }

    static enum Field {
        ID(0), TIMESTAMP(1), TYPE(2), PRODUCT(3), VERSION(4), PLATFORM(5), LOCALE(6),
        MANUFACTURER(7), DEVICE(8), URL(9), TEXT(10);
        public int i;

        Field(int c) {
            i = c;
        }
    }

    @Override
    public Iterator<Document> iterator() {
        return new OpinionsIterator(new TsvReader(in_));
    }

    private class OpinionsIterator implements Iterator<Document> {

        final TsvReader reader_;
        int i_ = 0;
        String[] row_;

        public OpinionsIterator(TsvReader reader) {
            reader_ = reader;
        }

        @Override
        public Document next() {
            @SuppressWarnings("serial")
            Document doc = new Document(
                    row_[Field.ID.i],
                    new HashMap<String, Object>() {{
                        for (Field f : Field.values())
                            put(f.name().toLowerCase(), row_[f.i]);
                    }});
            row_ = null;
            return doc;
        }

        @Override
        public boolean hasNext() {
            if (row_ != null)
                return true;
            try {
                row_ = reader_.nextRow();
                if (row_ == null)
                    return false;
                if (row_.length != Field.values().length) {
                    log.warn(
                        "L{} skipping record (wrong number of columns) {}\n",
                        i_, Arrays.toString(row_));
                    ++i_;
                    row_ = null;
                    return hasNext();
                }
                ++i_;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return true;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private static final Logger log = LoggerFactory.getLogger(OpinionStream.class);

    private final InputStream in_;

}
