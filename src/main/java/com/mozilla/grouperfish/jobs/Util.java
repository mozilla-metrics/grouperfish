package com.mozilla.grouperfish.jobs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.jobs.textcluster.TextClusterTool;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;
import com.mozilla.grouperfish.model.CollectionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores and reads Grouperfish configuration to/from hadoop config. This way
 * configuration values are transmitted to map/reduce tasks.
 */
public class Util {

	public Util(Conf conf) {
		Assert.nonNull(conf);
		conf_ = conf;
		tools_.put(ExportDocuments.NAME, ExportDocuments.class);
		tools_.put(VectorizeDocuments.NAME, VectorizeDocuments.class);
		tools_.put(TextClusterTool.NAME, TextClusterTool.class);
		tools_.put(Rebuild.NAME, Rebuild.class);
		tools_.put(RebuildAll.NAME, RebuildAll.class);
		vanillaHadoopConf_ = HBaseConfiguration.create();
	}

	/** @return The exit status of the job. */
	public int run(String toolName, String[] args) {
		Configuration hadoopConf = HBaseConfiguration.create();
		Tool tool;
		try {
			Class<? extends Tool> toolType = tools_.get(toolName);
			tool = toolType.getConstructor(Conf.class, Configuration.class).newInstance(
					new Object[] { conf_, hadoopConf });
		} catch (Exception e) {
			log.error("Could not instantiate the requested tool: " + toolName, e);
			return 1;
		}
		Assert.nonNull(tool);

		// Possibly merge the above with the Cli and handle hadoop config in the
		// abstract tool?
		String[] otherArgs;
		try {
			otherArgs = new GenericOptionsParser(hadoopConf, args).getRemainingArgs();
		} catch (IOException e) {
			log.error("Error running job: " + toolName, e);
			return 1;
		}
		try {
			return tool.run(otherArgs);
		} catch (Exception e) {
			log.error("Error running job: " + toolName, e);
			return 1;
		}
	}

	public void setJobTracker(Configuration hadoopConf, Collection collection) {
		final int DISTRIBUTION_THRESHOLD = 128000;
		final String setting = "mapred.job.tracker";
		hadoopConf.set(setting,
						collection.get(Attribute.SIZE) > DISTRIBUTION_THRESHOLD ?
						vanillaHadoopConf_.get(setting) : "local");
	}

	public Path outputDir(CollectionRef collection, long timestamp, CollectionTool tool) {
		return new Path(new StringBuilder().append(conf_.get(CONF_DFS_ROOT)).append('/')
				.append(Long.toString(timestamp)).append('/').append(mangle(collection.namespace())).append('_')
				.append(mangle(collection.key())).append('/').append(tool.name()).toString());
	}

	private String mangle(String source) {
		return DigestUtils.md5Hex(Bytes.toBytes(source)).substring(0, 8);
	}

	/**
	 * Stores the Grouperfish configuration into the hadoop configuration, so
	 * that it can be reconstructed from within Map/Reduce tasks.
	 */
	void saveConfToHadoopConf(Configuration hadoopConfig) {
		Assert.nonNull(hadoopConfig);
		hadoopConfig.set(HADOOP_CONF_KEY, conf_.toJSON());
	}

	/**
	 * Reads Grouperfish configuration from the hadoop configuration.
	 *
	 * @see #saveConfToHadoopConf(Configuration)
	 */
	static Conf fromHadoopConf(Configuration hadoopConfig) {
		Assert.nonNull(hadoopConfig);
		final String jsonConf = hadoopConfig.get(HADOOP_CONF_KEY);
		return (new com.mozilla.grouperfish.conf.Factory()).fromJSON(jsonConf);
	}

	private final Map<String, Class<? extends Tool>> tools_ = new HashMap<String, Class<? extends Tool>>();

	private static final Logger log = LoggerFactory.getLogger(Util.class);

	private final Conf conf_;
	private final Configuration vanillaHadoopConf_;

	private static final String HADOOP_CONF_KEY = "org.mozilla.grouperfish.conf";

	private static final String CONF_DFS_ROOT = "worker:dfs:root";

}
