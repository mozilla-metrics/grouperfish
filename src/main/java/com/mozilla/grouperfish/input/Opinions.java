package com.mozilla.grouperfish.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Opinions implements Iterable<String[]> {

	Opinions(final InputStream in) {
		in_ = in;
	}

	static enum Field {
		ID(0), TIMESTAMP(1), TYPE(2), PRODUCT(3), VERSION(4), PLATFORM(5), LOCALE(6), MANUFACTURER(7), DEVICE(8), URL(9), TEXT(
				10);
		public int i;

		Field(int c) {
			i = c;
		}
	}

	@Override
	public Iterator<String[]> iterator() {
		return new OpinionsIterator(new TsvReader(in_));
	}

	private class OpinionsIterator implements Iterator<String[]> {

		final TsvReader reader_;
		int i_ = 0;
		String[] row_;

		public OpinionsIterator(TsvReader reader) {
			reader_ = reader;
		}

		@Override
		public String[] next() {
			String[] r = row_;
			row_ = null;
			return r;
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
					log.warn("L{} skipping record (wrong number of columns) {}\n", i_, Arrays.toString(row_));
					++i_;
					next();
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

	private static final Logger log = LoggerFactory.getLogger(Opinions.class);

	private final InputStream in_;

}
