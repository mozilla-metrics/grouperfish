package com.mozilla.grouperfish.jobs.carrot2;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.ProcessingResult;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.jobs.AbstractCollectionTool;
import com.mozilla.grouperfish.jobs.ExportDocuments;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.ClusterRef;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;


public class CarrotClusterTool extends AbstractCollectionTool {

	public CarrotClusterTool(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	@Override
	public String name() {
		return NAME;
	}

	static final String NAME = "carrot_cluster_tool";

	/** Runs local carrot2 based clustering. Use only for small collections. */
	public List<Cluster> runLocal(Collection collection,
					  			  long timestamp,
					  			  Iterable<Document> documents) {

		Iterable<Document> docs = new ExportDocuments(conf_, getConf()).runLocal(collection.ref(), timestamp);
		final ArrayList<org.carrot2.core.Document> carrotDocs = new ArrayList<org.carrot2.core.Document>();
		for (final Document doc : docs) {
			org.carrot2.core.Document carrotDoc = new org.carrot2.core.Document(doc.text());
			carrotDoc.setField("id", doc.ref().id());
			carrotDocs.add(carrotDoc);
		}

		Class<?> algorithm = LingoClusteringAlgorithm.class;

		final Controller controller = ControllerFactory.createSimple();
		final ProcessingResult result = controller.process(carrotDocs, collection.ref().key(), algorithm);
		final List<org.carrot2.core.Cluster> clustersByTopic = result.getClusters();

		final List<Cluster> clusters = new ArrayList<Cluster>(clustersByTopic.size());
		for (org.carrot2.core.Cluster carrotCluster : clustersByTopic) {

			List<DocumentRef> related = new ArrayList<DocumentRef>(clustersByTopic.size());
			for (org.carrot2.core.Document carrotDoc : carrotCluster.getDocuments()) {
				related.add(new DocumentRef(collection.ref(), carrotDoc.getField("id").toString()));
			}
			clusters.add(new Cluster(new ClusterRef(collection.ref(),
										   timestamp,
										   carrotCluster.getLabel()), related));

		}

		return clusters;
	}

}
