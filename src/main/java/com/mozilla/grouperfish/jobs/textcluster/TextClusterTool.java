package com.mozilla.grouperfish.jobs.textcluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Importer;
import com.mozilla.grouperfish.jobs.AbstractCollectionTool;
import com.mozilla.grouperfish.jobs.CollectionTool;
import com.mozilla.grouperfish.jobs.Histogram;
import com.mozilla.grouperfish.jobs.VectorizeDocuments;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;
import com.mozilla.grouperfish.model.CollectionRef;


public class TextClusterTool extends AbstractCollectionTool {

	public TextClusterTool(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public int run(Collection collection, long timestamp) throws Exception {
		return runSequential(collection, timestamp);
	}

	@Override
	protected Job createSubmittableJob(CollectionRef collection, long timestamp) throws Exception {
		Assert.unreachable("Implement me!!!");
		return null;
	}

	protected int runSequential(Collection collection, long timestamp) throws Exception {
		// In memory version.
		// :TODO: Add mapper+reducer based on divide & conquer (chunk & merge),
		// maybe also based on more thorough parallelization.
		final CollectionTool source = new VectorizeDocuments(conf_, getConf());
		final Path inputDir = util_.outputDir(collection.ref(), timestamp, source);
		final Path p = new Path(inputDir, "tfidf-vectors/part-r-00000");

		List<VectorCluster> stage1 = fromVectors(p);
		List<VectorCluster> stage2 = merge(stage1);
		logHistogram(stage2);

		List<Cluster> clusters = new java.util.ArrayList<Cluster>(stage2.size());
		for (VectorCluster vectorCluster : stage2) {
			// :TODO: use LLR or something to make nice labels!
			// Until then we use the document label of the cluster medoid (the
			// id).
			clusters.add(vectorCluster.toCluster(collection.ref(), timestamp));
		}
		final Importer<Cluster> importer = new Factory(conf_).importer(Cluster.class);
		importer.load(clusters);

		// Rebuild complete: Activate changes in collection meta...
		final Importer<Collection> collectionImporter = new Factory(conf_).importer(Collection.class);
		collectionImporter.load(collection.set(Attribute.REBUILT, timestamp));

		return 0;
	}

	private List<VectorCluster> fromVectors(Path p) throws IOException {
		final List<VectorCluster> result = new java.util.ArrayList<VectorCluster>();
		SequenceFile.Reader reader = null;
		try {
			final Configuration hadoopConf = getConf();
			reader = new SequenceFile.Reader(p.getFileSystem(hadoopConf), p, hadoopConf);

			final Text key = (Text) ReflectionUtils.newInstance(reader.getKeyClass(), hadoopConf);

			final VectorWritable vector = (VectorWritable) ReflectionUtils.newInstance(reader.getValueClass(),
					hadoopConf);

			if (!reader.next(key, vector)) {
				log.warn("No input vectors found in ", p);
				return result;
			}

			final int cardinality = vector.get().size();
			List<VectorCluster> more;
			IndexClusterer clusterer = new IndexClusterer(cardinality);
			log.info("Starting clustering...");
			{
				do {
					more = clusterer.add(vector.get());
					if (more != null)
						result.addAll(more);
				} while (reader.next(key, vector));
				result.addAll(clusterer.clusters());
			}

			log.info("re-clustering remaining vectors...");
			{
				IndexClusterer restClusterer = new IndexClusterer(vector.get().size());
				for (Vector v : clusterer.rest()) {
					more = restClusterer.add(v);
					if (more != null)
						result.addAll(more);
				}
				result.addAll(restClusterer.clusters());
			}
		} finally {
			IOUtils.closeStream(reader);
		}
		return result;
	}

	private List<VectorCluster> merge(List<VectorCluster> result) {
		if (result.size() <= 1)
			return result;
		log.info("Starting meta-clustering...");
		final int cardinality = result.get(0).medoid().size();
		final Map<Vector, VectorCluster> sources = new HashMap<Vector, VectorCluster>(result.size());
		final IndexClusterer merger = new IndexClusterer(cardinality);
		final List<VectorCluster> metaClusters = new ArrayList<VectorCluster>();
		List<VectorCluster> more;
		for (VectorCluster c : result) {
			sources.put(c.medoid(), c);
			more = merger.add(c.medoid());
			if (more != null)
				metaClusters.addAll(more);
		}
		metaClusters.addAll(merger.clusters());

		List<VectorCluster> flatClusters = new ArrayList<VectorCluster>();
		for (final VectorCluster meta : metaClusters) {
			final Vector medoid = meta.medoid();
			final List<Vector> related = new ArrayList<Vector>();
			final List<Double> similarities = new ArrayList<Double>();
			related.addAll(sources.get(medoid).related());
			similarities.addAll(sources.get(medoid).similarities());

			int i = 0;
			for (final Vector substitue : meta.related()) {
				related.add(substitue);
				similarities.add(meta.similarities().get(i));
				related.addAll(sources.get(substitue).related());
				// :TODO: we should recompute these similarities for the new
				// medoid.
				similarities.addAll(sources.get(substitue).similarities());
				++i;
			}

			flatClusters.add(new VectorCluster(medoid, related, similarities));
		}
		return flatClusters;
	}

	private void logHistogram(List<VectorCluster> clustering) {
		final Histogram histogram = new Histogram();
		for (VectorCluster c : clustering)
			histogram.add(c.size(), c.size());
		log.info("Histogram: {}", histogram);
	}

	private static final Logger log = LoggerFactory.getLogger(TextClusterTool.class);

	public static final String NAME = "textcluster";

}
