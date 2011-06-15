package com.mozilla.grouperfish.jobs;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Importer;
import com.mozilla.grouperfish.jobs.carrot2.CarrotClusterTool;
import com.mozilla.grouperfish.jobs.textcluster.TextClusterTool;
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
		exporter_ = new ExportDocuments(conf_, getConf());
		clusterer_ = new CarrotClusterTool(conf_, getConf());
		importer_ = new Factory(conf_).importer(Cluster.class);
	}

	@Override
	public int run(Collection collection, long timestamp) throws Exception {
		new Util(conf_).setJobTracker(getConf(), collection);
		log.info("Rebuilding collection {} at {}", collection.ref().key(), timestamp);
		log.info("Size: {}", collection.get(Attribute.SIZE));

		// Use smart in-memory clustering for smallish collections:
		if (collection.get(Attribute.SIZE) < 1000*1000) {
			Iterable<Document> docs = exporter_.runLocal(collection, timestamp);
			List<Cluster> clusters = clusterer_.runLocal(collection, timestamp, docs);
			importer_.load(clusters);
			return 0;
		}

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
		return 0;
	}

	@Override
	public String name() {
		return NAME;
	}

	private static final Logger log = LoggerFactory.getLogger(Rebuild.class);

	static String NAME = "rebuild";

	private ExportDocuments exporter_;
	private CarrotClusterTool clusterer_;
	private Importer<Cluster> importer_;

}
