package com.mozilla.grouperfish.hbase.keys;

import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.ClusterRef;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.Document;
import com.mozilla.grouperfish.model.DocumentRef;

/**
 * Generates rowkeys. Instances must be threadsafe.
 *
 * For now, keys will be UTF-8 coded. We'll probably need to allow any byte[]
 * for keys for best space efficiency and performance.
 *
 * Whatever scheme is used to generate keys, it must work with the prefix
 * methods defined here, to allow for efficient scans.
 */
public abstract class Keys {

	public String documentPrefix(CollectionRef col) {
		return document(col.namespace(), col.key(), null);
	}

	public String clustersPrefix(String ns, String ck, long rebuildTS) {
		return cluster(ns, ck, rebuildTS, null);
	}

	public final String key(DocumentRef ref) {
		CollectionRef c = ref.ownerRef();
		return document(c.namespace(), c.key(), ref.id());
	}

	public final String key(CollectionRef ref) {
		return collection(ref.namespace(), ref.key());
	}

	public final String key(ClusterRef ref) {
		CollectionRef owner = ref.ownerRef();
		return cluster(owner.namespace(), owner.key(), ref.rebuildTs(), ref.label());
	}

	public final String key(Cluster cluster) {
		return key(cluster.ref());
	}

	public final String key(Document doc) {
		return key(doc.ref());
	}

	protected abstract String document(String ns, String ck, String docID);

	protected abstract String cluster(String ns, String ck, long rebuildTS, String label);

	protected abstract String collection(String ns, String ck);

}
