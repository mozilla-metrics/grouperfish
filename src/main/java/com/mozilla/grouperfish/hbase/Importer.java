package com.mozilla.grouperfish.hbase;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import com.mozilla.grouperfish.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parallel importer of stuff into Hbase.
 */
public class Importer<T extends Model> {

	public Importer(final Factory factory, final Class<T> model) {
		model_ = model;
		factory_ = factory;
		tableName_ = factory.tableName(model);
	}

	public void load(Iterable<T> input) {

		log.debug("Starting import into table {}", tableName_);
		final ExecutorService workers = workers();

		// So modulo does not match right away, we set i != 0
		int i = 1;
		List<T> batch = new ArrayList<T>(BATCH_SIZE);
		for (T item : input) {
			batch.add(item);
			if (i % BATCH_SIZE == 0) {
				workers.submit(new Insert(batch));
				batch = new ArrayList<T>(BATCH_SIZE);
			}
			if (i % 50000 == 0) {
				log.info("Queued {} messages for table {}", i, tableName_);
			}
			++i;
		}
		workers.submit(new Insert(batch));

		// Submit will block until it is safe to shut down:
		shutdownGracefully(workers);
		try {
			for (HTableInterface table : tables_) {
				table.flushCommits();
				factory_.release(table);
			}
		} catch (IOException flushFail) {
			log.error("Could not import buffer!", flushFail);
		}
	}

	public void load(T item) throws IOException {
		Put put = Adapters.create(factory_, model_).put(item);
		factory_.table(model_).put(put);
	}

	/**
	 * So there is this factory where all workers do is running and then relax
	 * at the pool, and where all clients must wait in a queue. It is a pretty
	 * fun work environment... until everyone gets garbage collected that is.
	 */
	private ExecutorService workers() {
		return new ThreadPoolExecutor(10, 20, 90, TimeUnit.SECONDS,

		new ArrayBlockingQueue<Runnable>(100),

		new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final HTableInterface workerTable = factory_.table(model_);
				tables_.add(workerTable);

				Thread worker = new Thread(r) {
					@Override
					public void run() {
						table_.set(workerTable);
						super.run();
					}
				};

				worker.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(Thread t, Throwable e) {
						log.error("Uncaught exception from importer worker.", e);
					}
				});
				return worker;
			}
		},

		new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
				try {
					executor.getQueue().put(task);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	private void shutdownGracefully(final ExecutorService pool) {
		pool.shutdown();
		try {
			if (pool.awaitTermination(120, TimeUnit.SECONDS))
				return;
			pool.shutdownNow();
			if (pool.awaitTermination(60, TimeUnit.SECONDS))
				return;
			log.error("Importer pool did not terminate within timeout.");
			System.exit(1);
		} catch (InterruptedException e) {
			pool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	/** Each insert task submits a batch of items */
	private class Insert implements Runnable {

		private final List<T> items_;
		private final RowAdapter<T> adapter_;

		Insert(final List<T> items) {
			items_ = items;
			adapter_ = items_.size() > 0 ? Adapters.create(factory_, model_) : null;
		}

		@Override
		public void run() {
			if (items_.size() == 0)
				return;
			List<Put> batch = new ArrayList<Put>(items_.size());
			for (T item : items_) {
				batch.add(adapter_.put(item));
			}
			try {
				table_.get().put(batch);
			} catch (IOException e) {
				String from = adapter_.key(items_.get(0));
				String to = adapter_.key(items_.get(items_.size() - 1));
				log.error(String.format("While inserting batch %s,%s", from, to));
				log.error("IO Error in importer", e);
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger(Importer.class);

	private static final int BATCH_SIZE = 1000;

	private final Factory factory_;
	private final Class<T> model_;
	private final String tableName_;

	/** Thread local tables that are reused by each worker. */
	private final ThreadLocal<HTableInterface> table_ = new ThreadLocal<HTableInterface>();

	/**
	 * We have no guarantee on destructors being called, and no access to the
	 * threadlocal {@link #table_} instance from the parent thread, but we need
	 * to release the HTables in long running programs: To this end, when
	 * spawning a worker, the thread pool adds a reference to this list, so the
	 * HTables used by the workers can be released upon completion.
	 */
	final List<HTableInterface> tables_ = new java.util.LinkedList<HTableInterface>();

}
