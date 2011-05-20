package org.mozilla.grouperfish.jobs.textcluster;

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
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.mozilla.grouperfish.base.Assert;
import org.mozilla.grouperfish.conf.Conf;
import org.mozilla.grouperfish.hbase.Factory;
import org.mozilla.grouperfish.hbase.Importer;
import org.mozilla.grouperfish.jobs.AbstractCollectionTool;
import org.mozilla.grouperfish.jobs.CollectionTool;
import org.mozilla.grouperfish.jobs.Histogram;
import org.mozilla.grouperfish.jobs.VectorizeDocuments;
import org.mozilla.grouperfish.model.BaseCluster;
import org.mozilla.grouperfish.model.Cluster;
import org.mozilla.grouperfish.model.ClusterRef;
import org.mozilla.grouperfish.model.Collection;
import org.mozilla.grouperfish.model.CollectionRef;
import org.mozilla.grouperfish.model.Collection.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TextClusterTool extends AbstractCollectionTool {

  public
  TextClusterTool(Conf conf, Configuration hadoopConf) {
    super(conf, hadoopConf);
  }


  @Override public
  String name() { return NAME; }


  @Override public
  int run(CollectionRef collection, long timestamp) throws Exception {
    return run(collection, timestamp, true);
  }


  @Override protected
  Job createSubmittableJob(CollectionRef collection, long timestamp)
  throws Exception {
    Assert.unreachable("Implement me!!!");
    return null;
  }


  protected
  int run(CollectionRef collection, long timestamp, boolean sequential)
      throws Exception {
    if (!sequential) return super.run(collection, timestamp);

    // In memory version.
    // :TODO: Add mapper+reducer based on divide & conquer (chunk & merge),
    //        maybe also based on more thorough parallelization.
    final CollectionTool source = new VectorizeDocuments(conf_, getConf());
    final Path inputDir = util_.outputDir(collection, timestamp, source);
    final Path p = new Path(inputDir, "tfidf-vectors/part-r-00000");

    List<BaseCluster> stage1 = fromVectors(p);
    List<BaseCluster> stage2 = merge(stage1);
    logHistogram(stage2);

    List<Cluster> clusters = new java.util.ArrayList<Cluster>(stage2.size());
    for (BaseCluster cluster : stage2) {
      // :TODO: use LLR or something to make nice labels!
      // Until then we use the document label of the cluster medoid (the id).
      final String label = ((NamedVector) cluster.medoid()).getName();
      final ClusterRef ref = new ClusterRef(collection, timestamp, label);
      clusters.add(new Cluster(ref, cluster));
    }
    final Importer<Cluster> importer =
      new Factory(conf_).importer(Cluster.class);
    importer.load(clusters);

    // Rebuild complete: Activate changes in collection meta...
    final Importer<Collection> collectionImporter =
      new Factory(conf_).importer(Collection.class);
    collectionImporter.load(new Collection(collection).set(Attribute.REBUILT,
                                                           timestamp));

    return 0;
  }


  private
  List<BaseCluster> fromVectors(Path p) throws IOException {
    final List<BaseCluster> result = new java.util.ArrayList<BaseCluster>();
    SequenceFile.Reader reader = null;
    try {
      final Configuration hadoopConf = getConf();
      reader = new SequenceFile.Reader(p.getFileSystem(hadoopConf),
                                       p, hadoopConf);

      final Text key =
        (Text) ReflectionUtils.newInstance(reader.getKeyClass(), hadoopConf);

      final VectorWritable vector =
        (VectorWritable) ReflectionUtils.newInstance(reader.getValueClass(),
                                                     hadoopConf);

      if (!reader.next(key, vector)) {
        log.warn("No input vectors found in ", p);
        return result;
      }

      final int cardinality = vector.get().size();
      List<BaseCluster> more;
      IndexClusterer clusterer = new IndexClusterer(cardinality);
      log.info("Starting clustering...");
      {
        do {
          more = clusterer.add(vector.get());
          if (more != null) result.addAll(more);
        } while (reader.next(key, vector));
        result.addAll(clusterer.clusters());
      }

      log.info("re-clustering remaining vectors...");
      {
        IndexClusterer restClusterer = new IndexClusterer(vector.get().size());
        for (Vector v : clusterer.rest()) {
          more = restClusterer.add(v);
          if (more != null) result.addAll(more);
        }
        result.addAll(restClusterer.clusters());
      }
    }
    finally {
      IOUtils.closeStream(reader);
    }
    return result;
  }


  private
  List<BaseCluster> merge(List<BaseCluster> result) {
    if (result.size() <= 1) return result;
    log.info("Starting meta-clustering...");
    final int cardinality = result.get(0).medoid().size();
    final Map<Vector, BaseCluster> sources =
      new HashMap<Vector, BaseCluster>(result.size());
    final IndexClusterer merger = new IndexClusterer(cardinality);
    final List<BaseCluster> metaClusters = new ArrayList<BaseCluster>();
    List<BaseCluster> more;
    for (BaseCluster c : result) {
      sources.put(c.medoid(), c);
      more = merger.add(c.medoid());
      if (more != null) metaClusters.addAll(more);
    }
    metaClusters.addAll(merger.clusters());

    List<BaseCluster> flatClusters = new ArrayList<BaseCluster>();
    for (final BaseCluster meta : metaClusters) {
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
        // :TODO: we should recompute these similarities for the new medoid.
        similarities.addAll(sources.get(substitue).similarities());
        ++i;
      }

      flatClusters.add(new BaseCluster(medoid, related, similarities));
    }
    return flatClusters;
  }


  private
  void logHistogram(List<BaseCluster> clustering) {
    final Histogram histogram = new Histogram();
    for (BaseCluster c : clustering) histogram.add(c.size(), c.size());
    log.info("Histogram: {}", histogram);
  }


  private static final Logger log =
    LoggerFactory.getLogger(TextClusterTool.class);

  public static final String NAME = "textcluster";

}
