package com.mozilla.grouperfish.jobs;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;

import com.mozilla.grouperfish.conf.Conf;
import com.mozilla.grouperfish.hbase.Factory;
import com.mozilla.grouperfish.hbase.Importer;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.Collection.Attribute;

public class LoadClusters extends AbstractCollectionTool {

	public LoadClusters(Conf conf, Configuration hadoopConf) {
		super(conf, hadoopConf);
	}

	public int runLocal(final Collection collection,
						long timestamp,
						final Iterable<Cluster> clusters) {

		final Importer<Cluster> importer = new Factory(conf_).importer(Cluster.class);
		importer.load(clusters);


		// Rebuild complete: Activate changes in collection meta...
		final Importer<Collection> collectionImporter = new Factory(conf_).importer(Collection.class);

		try {
			collectionImporter.load(collection.set(Attribute.REBUILT, timestamp));
		}
		catch (IOException ex) {

			return 1;
		}

		return 0;
	}

	static final String NAME = "load_clusters";

	public String name() {
		return NAME;
	}

}
