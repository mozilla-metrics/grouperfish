package com.mozilla.grouperfish.jobs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.CollectionAdapter;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Schema.Documents;
import com.mozilla.grouperfish.hbase.Source;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Document;


/**
 * Export all documents into a directory, one file per map-task.
 *
 * This is part of the full rebuild and a prerequisite for vectorization.
 *
 * TODO: We want a better partitioner so that regions are only looked at by a
 * mapper if they overlap with our prefix.
 */
public class ExportDocuments extends AbstractCollectionTool {

	final static String NAME = "export_documents";

	static class ExportMapper extends TableMapper<Text, Text> {
		public static enum Counters {
			ROWS_USED
		}

		@Override
		protected void map(ImmutableBytesWritable key, Result row, ExportMapper.Context context)
				throws java.io.IOException, InterruptedException {
			context.getCounter(Counters.ROWS_USED).increment(1);
			byte[] documentID = row.getColumnLatest(Documents.Main.FAMILY, Documents.Main.ID.qualifier).getValue();
			KeyValue text = row.getColumnLatest(Documents.Main.FAMILY, Documents.Main.TEXT.qualifier);
			context.write(new Text(documentID), new Text(text.getValue()));
		};
	}

	public ExportDocuments(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	public Iterable<Document> runLocal(Collection collection, long timestamp) {
		final Factory factory = new Factory(conf_);
		return new CollectionAdapter(factory).documents(collection.ref());
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
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job, outputDir);

		// Set optional scan parameters
		final Factory factory = new Factory(conf_);
		final Source<Document> documents = new CollectionAdapter(factory).documents(ref);

		TableMapReduceUtil.initTableMapperJob(
				factory.tableName(Document.class),
				documents.getScan(), ExportMapper.class, null, null, job);
		return job;
	}

	@Override
	public String name() {
		return NAME;
	}

}
