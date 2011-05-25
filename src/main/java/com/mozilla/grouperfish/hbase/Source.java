package com.mozilla.grouperfish.hbase;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import com.mozilla.grouperfish.model.Model;

/**
 * Reads all model objects of a type from HBase. We'll add filter criteria as
 * needed.
 */
public class Source<T extends Model> implements Iterable<T> {

	public Source(final Factory factory, final Class<T> model) {
		model_ = model;
		factory_ = factory;
		table_ = factory.table(model);
	}

	@Override
	public Iterator<T> iterator() {
		try {
			return new ModelIterator<T>(Adapters.create(factory_, model_), table_.getScanner(new Scan()));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not create scanner", e);
		}
	}

	static class ModelIterator<S extends Model> implements Iterator<S> {

		public ModelIterator(RowAdapter<S> adapter, ResultScanner scanner) {
			adapter_ = adapter;
			it_ = scanner.iterator();
		}

		@Override
		public boolean hasNext() {
			return it_.hasNext();
		}

		@Override
		public S next() {
			if (!it_.hasNext())
				throw new NoSuchElementException("Scanner empty!");
			return adapter_.read(it_.next());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private final Iterator<Result> it_;
		private final RowAdapter<S> adapter_;
	};

	private final Factory factory_;
	private final Class<T> model_;
	private final HTableInterface table_;

}
