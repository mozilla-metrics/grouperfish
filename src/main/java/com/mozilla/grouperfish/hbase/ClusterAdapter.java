package com.mozilla.grouperfish.hbase;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.hbase.Schema.Clusters;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.ClusterRef;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.DocumentRef;
import com.mozilla.grouperfish.model.Ref;

public class ClusterAdapter implements RowAdapter<Cluster> {

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

	@Override
	public Cluster read(Result next) {
		return Assert.unreachable(Cluster.class, "Not implemented.");
	}

	@Override public Get get(Ref<Cluster> ref) {
		Assert.check(ref instanceof ClusterRef);
		return get((ClusterRef) ref);
	}

	public Get get(ClusterRef ref) {
		return new Get(Bytes.toBytes(factory_.keys().key(ref)));
	}

}