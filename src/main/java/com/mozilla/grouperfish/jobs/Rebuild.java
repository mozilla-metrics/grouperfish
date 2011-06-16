package com.mozilla.grouperfish.jobs;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.CollectionAdapter;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Importer;
import com.mozilla.grouperfish.jobs.carrot2.CarrotClusterTool;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;
import com.mozilla.grouperfish.model.Document;

/**
 * Completely rebuilds a collection using the appropriate algorithm for every
 * configured clustering configuration.
 */
public class Rebuild extends AbstractCollectionTool {

	public Rebuild(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
		hbase_ = new Factory(conf_);
		new ExportDocuments(conf_, getConf());
		clusterer_ = new CarrotClusterTool(conf_, getConf());
		clusterImporter_ = hbase_.importer(Cluster.class);
		collectionImporter_ = hbase_.importer(Collection.class);
	}

	public boolean needsProcessing(Collection c, String namespace) {
		final Long rebuilt = c.get(Attribute.REBUILT);
		final Long modified = c.get(Attribute.MODIFIED);
		// TODO:
		// Push these attributes down to the HBase scannners, so number of collections can
		// grow beyond mere thousands.
		if (modified == null) {
			log.info("Ignoring collection '{} / {}' because it has no modification date set.",
					 c.ref().namespace(), c.ref().key());
			return false;
		}
		if (rebuilt != null && rebuilt > modified) {
			log.info("Ignoring collection '{} / {}' because of no growth since last rebuild.",
					 c.ref().namespace(), c.ref().key());
			return false;
		}
		if (namespace != null && !namespace.equals(c.ref().namespace())) {
			log.info("Ignoring collection '{} / {}' because namespace != " + namespace,
					 c.ref().namespace(), c.ref().key());
			return false;
		}
		return true;
	}

	@Override
	public int run(Collection collection, long timestamp) throws Exception {

		if (!needsProcessing(collection, null)) return 0;

		log.info("Rebuilding collection {} at {}", collection.ref().key(), timestamp);

		// :TODO: this is inefficient- the frontend needs to maintain the collection size counter...
		// (needs atomic increment)
		long size = 0;
		List<Document> docs = new ArrayList<Document>();
		for (Document doc : new CollectionAdapter(hbase_).documents(collection.ref(), timestamp)) {
			docs.add(doc);
			++size;
		}
		collection.set(Attribute.SIZE, size);
		log.info("Updated collection size to: {}", size);

		List<Cluster> clusters = clusterer_.runLocal(collection, timestamp, docs);
		clusterImporter_.load(clusters);

		// Rebuild complete: Activate changes in collection meta...
		collection.set(Attribute.REBUILT, timestamp);
		collectionImporter_.load(collection);


		/*
		final CollectionTool[] toolchain = new CollectionTool[] {
				new ExportDocuments(conf_, getConf()),
				new VectorizeDocuments(conf_, getConf()),
				new TextClusterTool(conf_, getConf())
		};

		for (final CollectionTool tool : toolchain) {
			int returnCode = tool.run(collection, timestamp);
			if (returnCode != 0) {
				log.error("Error running job {}", tool.name());
				return returnCode;
			}
		}
		*/
		return 0;
	}

	@Override
	public String name() {
		return NAME;
	}

	private static final Logger log = LoggerFactory.getLogger(Rebuild.class);

	static String NAME = "rebuild";

	private final Factory hbase_;
	private final CarrotClusterTool clusterer_;
	private final Importer<Cluster> clusterImporter_;
	private final Importer<Collection> collectionImporter_;

}
