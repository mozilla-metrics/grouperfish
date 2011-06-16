package com.mozilla.grouperfish.jobs.carrot2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.jobs.AbstractCollectionTool;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.ClusterRef;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;


public class CarrotClusterTool extends AbstractCollectionTool {

	private static final Logger log = LoggerFactory.getLogger(CarrotClusterTool.class);

	public CarrotClusterTool(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	@Override
	public String name() {
		return NAME;
	}

	static final String NAME = "carrot_cluster_tool";
	private static final Object OTHERS = "Other Topics";

	//private static int LIMIT_LINGO = 700;
	private static int LIMIT_CLUSTERING = 3000;

	/** Runs local carrot2 based clustering. Use only for small collections. */
	public List<Cluster> runLocal(Collection collection,
					  			  long timestamp,
					  			  Iterable<Document> source) {

		final long n = collection.get(Attribute.SIZE).longValue();

		final Index<Cluster> index;

		final Iterable<Document> docs;
		if (n < LIMIT_CLUSTERING) {
			docs = source;
			index = null;
		}
		else {
			 ArrayList<Document> shuffled = new ArrayList<Document>();
			 for (Document doc : source) shuffled.add(doc);
			 randomizeSubList(shuffled, LIMIT_CLUSTERING);
			 docs = shuffled;
			 index = new Index<Cluster>();
		}


		final Iterator<Document> it = docs.iterator();
		int docsProcessed = 0;
		int numOthers = 0;
		final List<Cluster> clusters;

		// Part 1: Calculate clusters using Carrot2 algorithms
		{
			final long start = System.currentTimeMillis();

			final ArrayList<org.carrot2.core.Document> carrotDocs =
				new ArrayList<org.carrot2.core.Document>();

			while (it.hasNext()) {
				Document next = it.next();
				org.carrot2.core.Document carrotDoc = new org.carrot2.core.Document(next.text());
				carrotDoc.setField("id", next.ref().id());
				carrotDocs.add(carrotDoc);
				++docsProcessed;
				if (docsProcessed >= LIMIT_CLUSTERING) break;
			}

			final Class<?> algorithm = LingoClusteringAlgorithm.class;

			final Controller controller = ControllerFactory.createSimple();
			final ProcessingResult result = controller.process(carrotDocs, collection.ref().key(), algorithm);
			final List<org.carrot2.core.Cluster> clustersByTopic = result.getClusters();

			clusters = new ArrayList<Cluster>(clustersByTopic.size());
			for (org.carrot2.core.Cluster carrotCluster : clustersByTopic) {
				final List<DocumentRef> related = new ArrayList<DocumentRef>(clustersByTopic.size());
				for (org.carrot2.core.Document carrotDoc : carrotCluster.getDocuments()) {
					related.add(new DocumentRef(collection.ref(), carrotDoc.getField("id").toString()));
				}
				final String label = carrotCluster.getLabel();
				final Cluster cluster =
					new Cluster(new ClusterRef(collection.ref(), timestamp, label), related);
				clusters.add(cluster);

				if (label.equals(OTHERS)) {
					numOthers = carrotCluster.size();
				}
				else if (index != null) {
					for (org.carrot2.core.Document carrotDoc : carrotCluster.getDocuments()) {
						index.add(carrotDoc.getTitle(), label, cluster);
					}
				}
			}

			log.info("Carrot2 results in {}ms: {} docs, {} clusters, {} in Other Topics",
					 new Object[]{System.currentTimeMillis() - start, docsProcessed,
					 			  clusters.size(), numOthers});
		}

		if (n <= LIMIT_CLUSTERING) return clusters;

		// Part 2: Associate remaining documents.
		{
			final long start = System.currentTimeMillis();

			double[] scoreContainer = new double[1];
			while (it.hasNext()) {
				Document next = it.next();
				Cluster mostSimilar = index.find(next.text(), scoreContainer);
				if (mostSimilar != null) {
					mostSimilar.documents().add(next.ref());
					mostSimilar.similarities().add(scoreContainer[0]);
				}
				else {
					++numOthers;
				}
				++docsProcessed;
			}

			log.info("Carrot2:Similarity results in {}ms: {} docs, {} clusters, {} in Other Topics",
					 new Object[]{System.currentTimeMillis() - start, docsProcessed,
					 			  clusters.size(), numOthers});
		}

		return clusters;
	}


	/**
	 * Pick k random elements as head for the input list.
	 * Previous values are swapped towards the end, in place.
	 * Operates in-place. The full list is returned.
	 */
	private <T> void randomizeSubList(final List<T> input, final int k)
	{
	    final Random r = new Random();
	    final int n = input.size();
	    for (int i = 0; i < k; i++) {
	        int indexToSwap = i + r.nextInt(n - i);
	        T temp = input.get(i);
	        input.set(i, input.get(indexToSwap));
	        input.set(indexToSwap, temp);
	    }
	}

}
