package com.mozilla.grouperfish.jobs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.ClusterAdapter;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Source;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.CollectionRef;


/**
 * Export clusters into a tsv file (cluster key, document text)
 */
public class ExportClusters extends AbstractCollectionTool {

	final static String NAME = "export_clusters";

	static class ExportMapper extends TableMapper<Text, Text> {
		public static enum Counters {
			ROWS_USED
		}

		private Factory factory_;
		public void setup(Context context) {
			factory_ = new Factory(Util.fromHadoopConf(context.getConfiguration()));
		}

		@Override
		protected void map(ImmutableBytesWritable key, Result row, ExportMapper.Context context)
				throws java.io.IOException, InterruptedException {
			context.getCounter(Counters.ROWS_USED).increment(1);
			ClusterAdapter adapter = new ClusterAdapter(factory_);
			final Cluster c = adapter.read(row);
			for (int i = 0; i < c.documents().size(); ++i) {
				context.write(new Text(c.ref().label()), new Text(c.documents().get(i).id()));
			}

		};
	}

	public ExportClusters(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	@Override
	protected Job createSubmittableJob(CollectionRef ref, long timestamp) throws Exception {
		final Configuration hadoopConf = this.getConf();
		new Util(conf_).saveConfToHadoopConf(hadoopConf);

		final Path outputDir = util_.outputDir(ref, timestamp, this);
		final String jobName = jobName(ref, timestamp);
		final Job job = new Job(hadoopConf, jobName);

		job.setJarByClass(AbstractCollectionTool.class);
		job.setNumReduceTasks(0);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job, outputDir);

		// Set optional scan parameters
		final Factory factory = new Factory(conf_);
		final Source<Cluster> clusters = new ClusterAdapter(factory).all(ref, timestamp);

		TableMapReduceUtil.initTableMapperJob(
				factory.tableName(Cluster.class),
				clusters.getScan(), ExportMapper.class, null, null, job);

		return job;
	}

	@Override
	public String name() {
		return NAME;
	}

}
