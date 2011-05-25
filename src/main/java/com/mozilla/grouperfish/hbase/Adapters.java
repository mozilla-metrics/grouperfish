package com.mozilla.grouperfish.hbase;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import com.mozilla.grouperfish.base.Assert;
import com.mozilla.grouperfish.hbase.Schema.Clusters;
import com.mozilla.grouperfish.hbase.Schema.Documents;
import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.Collection;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;
import com.mozilla.grouperfish.model.Model;

class Adapters {

	@SuppressWarnings("unchecked")
	static public <S extends Model> RowAdapter<S> create(final Factory factory, final Class<S> model) {
		if (model == Cluster.class) {
			return (RowAdapter<S>) new ClusterAdapter(factory);
		}
		if (model == Collection.class) {
			return (RowAdapter<S>) new CollectionAdapter(factory);
		}
		if (model == Document.class) {
			return (RowAdapter<S>) new DocumentAdapter(factory);
		}
		return Assert.unreachable(RowAdapter.class);
	}

	static class ClusterAdapter implements RowAdapter<Cluster> {

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

			// Medoid
			put.add(Clusters.Documents.FAMILY, Bytes.toBytes(cluster.representativeDoc().id()), Bytes.toBytes(1.0));

			// Contents
			int i = 0;
			for (DocumentRef doc : cluster.relatedDocs()) {
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
	}

	static class DocumentAdapter implements RowAdapter<Document> {

		final Factory factory_;

		public DocumentAdapter(Factory factory) {
			factory_ = factory;
		}

		@Override
		public Put put(Document doc) {
			if (doc.text().length() == 0)
				return null;
			CollectionRef owner = doc.ref().ownerRef();
			return new Put(Bytes.toBytes(key(doc)))
					.add(Documents.Main.FAMILY, Documents.Main.NAMESPACE.qualifier, Bytes.toBytes(owner.namespace()))
					.add(Documents.Main.FAMILY, Documents.Main.COLLECTION_KEY.qualifier, Bytes.toBytes(owner.key()))
					.add(Documents.Main.FAMILY, Documents.Main.ID.qualifier, Bytes.toBytes(doc.ref().id()))
					.add(Documents.Main.FAMILY, Documents.Main.TEXT.qualifier, Bytes.toBytes(doc.text()));
		}

		@Override
		public String key(Document doc) {
			return factory_.keys().key(doc);
		}

		@Override
		public Document read(Result next) {
			return Assert.unreachable(Document.class, "Not implemented.");
		}
	}
}
