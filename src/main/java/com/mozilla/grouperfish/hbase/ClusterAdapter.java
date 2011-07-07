package com.mozilla.grouperfish.hbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.hbase.Schema.Clusters;
import com.mozilla.grouperfish.hbase.Schema.Collections.Main;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.ClusterRef;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.DocumentRef;
import com.mozilla.grouperfish.model.Ref;

public class ClusterAdapter implements RowAdapter<Cluster> {

	public static final Logger log = LoggerFactory.getLogger(ClusterAdapter.class);

	final Factory factory_;

	public ClusterAdapter(Factory factory) {
		factory_ = factory;
	}

	@Override
	public Put put(Cluster cluster) {
		final CollectionRef owner = cluster.ref().ownerRef();
		final Put put = new Put(Bytes.toBytes(key(cluster)));

		// Meta information
		put.add(Clusters.Main.FAMILY, Clusters.Main.NAMESPACE.qualifier, Bytes.toBytes(owner.namespace()))
				.add(Clusters.Main.FAMILY, Clusters.Main.KEY.qualifier, Bytes.toBytes(owner.key()))
				.add(Clusters.Main.FAMILY, Clusters.Main.TIMESTAMP.qualifier,
						Bytes.toBytes(cluster.ref().rebuildTs()))
				.add(Clusters.Main.FAMILY, Clusters.Main.LABEL.qualifier, Bytes.toBytes(cluster.ref().label()));

		// Contents
		int i = 0;
		for (DocumentRef doc : cluster.documents()) {
			put.add(Clusters.Documents.FAMILY, Bytes.toBytes(doc.id()),
					Bytes.toBytes(cluster.similarities().get(i).toString()));
			++i;
		}

		return put;
	}

	@Override
	public String key(Cluster cluster) {
		return factory_.keys().key(cluster);
	}

	public Source<Cluster> all(CollectionRef ref, long timestamp) {
		final Scan scan = new Scan();
		scan.setMaxVersions(1);
		final String prefix = factory_.keys().clustersPrefix(ref.namespace(), ref.key(), timestamp);
		scan.setFilter(new PrefixFilter(Bytes.toBytes(prefix)));
		log.debug("Looking up clusters by key prefix {}", prefix);

		return new Source<Cluster>(factory_, Cluster.class, scan);
	}

	@Override
	public Cluster read(Result result) {
		if (result.isEmpty()) return null;

		final Map<byte[], byte[]> main = result.getFamilyMap(Main.FAMILY);
		final CollectionRef ownerRef;
		{
			final byte[] ns = main.get(Main.NAMESPACE.qualifier);
			final byte[] key = main.get(Main.KEY.qualifier);
			ownerRef = new CollectionRef(Bytes.toString(ns), Bytes.toString(key));
		}

		final List<DocumentRef> members = new ArrayList<DocumentRef>();
		final List<Double> similarities = new ArrayList<Double>();
		final Map<byte[], byte[]> docs = result.getFamilyMap(Clusters.Documents.FAMILY);
		for (Map.Entry<byte[], byte[]> entry : docs.entrySet()) {
			final String docId = Bytes.toString(entry.getKey());
			final Double score = Double.valueOf(Bytes.toString(entry.getValue()));
			members.add(new DocumentRef(ownerRef, docId));
			similarities.add(score);
		}

		final Cluster c;
		{
			final byte[] ts = main.get(Clusters.Main.TIMESTAMP.qualifier);
			final byte[] label = main.get(Clusters.Main.LABEL.qualifier);
			final ClusterRef ref = new ClusterRef(ownerRef, Bytes.toLong(ts), Bytes.toString(label));
			c = new Cluster(ref, members, similarities);
		}

		return c;
	}

	@Override public Get get(Ref<Cluster> ref) {
		Assert.check(ref instanceof ClusterRef);
		return get((ClusterRef) ref);
	}

	public Get get(ClusterRef ref) {
		return new Get(Bytes.toBytes(factory_.keys().key(ref)));
	}

}