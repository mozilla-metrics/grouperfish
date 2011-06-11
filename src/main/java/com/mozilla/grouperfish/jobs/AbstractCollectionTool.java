package com.mozilla.grouperfish.jobs;

import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.CollectionRef;

/**
 * A hadoop tool that operates on a collection of documents. Collection tools
 * should be stateless (except for having configuration).
 */
public abstract class AbstractCollectionTool extends Configured implements Tool, CollectionTool {

	public AbstractCollectionTool(Conf conf, Configuration hadoopConf) {
		Assert.nonNull(conf, hadoopConf);
		this.setConf(hadoopConf);
		conf_ = conf;
		util_ = new Util(conf);
	}

	@Override
	public int run(String[] args) throws Exception {
		if (args.length > 0 && "help".equals(args[0]))
			return usage(0);

		long timestamp = new Date().getTime();
		if (args.length == 4 && "--timestamp".equals(args[2])) {
			timestamp = Long.valueOf(args[3]);
		} else if (args.length > 3) {
			return usage(1);
		}

		CollectionRef ref = new CollectionRef(args[0], args[1]);
		Collection collection = new Factory(conf_).source(Collection.class).get(ref);
		return run(collection, timestamp);
	}

	/**
	 * @see CollectionTool#run(CollectionRef, long)
	 */
	@Override
	public int run(Collection collection, long timestamp) throws Exception {

		final Path dest = util_.outputDir(collection.ref(), timestamp, this);
		FileSystem fs = FileSystem.get(dest.toUri(), getConf());
		if (fs.exists(dest)) {
			// Should not happen in everyday usage, due to locking + timestamp.
			// Can very well happen during development.
			log.warn("Output dir {} already exists! Pruning.", dest);
			fs.delete(dest, true);
		}

		new Util(conf_).setJobTracker(getConf(), collection);

		Job job = createSubmittableJob(collection.ref(), timestamp);
		log.info("Running job: {} using jobtracker: {}",
				 job.getJobName(), getConf().getStringCollection("mapred.job.tracker"));
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public String jobName(CollectionRef c, long timestamp) {
		return String.format("Grouperfish:%s %s/%s/%s", name(), timestamp, c.namespace(), c.key());
	}

	/**
	 * Create a job to run on hadoop.
	 *
	 * Tools can implement this to create their job, or override
	 * {@link #run(CollectionRef, long)}.
	 */
	protected Job createSubmittableJob(CollectionRef collection, long timestamp) throws Exception {
		return Assert.unreachable(Job.class);
	}

	/** Possibly tool-specific usage info. %s is replaced with the tool name. */
	protected String synopsis() {
		return "%s NAMESPACE COLLECTION_KEY\n";
	}

	protected int usage(int status) {
		(status == 0 ? System.out : System.err).format(synopsis(), name());
		return status;
	}

	protected final Conf conf_;
	protected final Util util_;

	private static final Logger log = LoggerFactory.getLogger(AbstractCollectionTool.class);

}
