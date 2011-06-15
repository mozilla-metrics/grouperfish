package com.mozilla.grouperfish.jobs.textcluster;

import java.util.List;

import org.apache.mahout.math.Vector;

import com.mozilla.grouperfish.model.Cluster;
import com.mozilla.grouperfish.model.ClusterRef;
import com.mozilla.grouperfish.model.CollectionRef;
import com.mozilla.grouperfish.model.DocumentRef;

/**
 * A stripped down representation of a cluster, without notion of references and
 * collections.
 */
class VectorCluster {

	public Vector medoid() {
		return medoid_;
	}

	public int size() {
		return related_.size();
	}

	public final List<Vector> related() {
		return related_;
	};

	public final List<Double> similarities() {
		return similarities_;
	};

	public VectorCluster(Vector medoid, List<Vector> related, List<Double> similarities) {
		medoid_ = medoid;
		related_ = related;
		similarities_ = similarities;
	}

	public VectorCluster(VectorCluster data) {
		medoid_ = data.medoid_;
		related_ = data.related_;
		similarities_ = data.similarities_;
	}

	private final Vector medoid_;
	private final List<Vector> related_;
	private final List<Double> similarities_;


	public Cluster toCluster(CollectionRef ownerRef, long timestamp) {

		DocumentRef representativeDoc = new DocumentRef(ownerRef,
														medoid().getName());

		List<DocumentRef> documents = new java.util.ArrayList<DocumentRef>();
		documents.add(representativeDoc);
		for (Vector v : related()) {
			documents.add(new DocumentRef(ownerRef, v.getName()));
		}
		ClusterRef ref = new ClusterRef(ownerRef, timestamp, representativeDoc.id());
		return new Cluster(ref, documents, similarities_);
	}
}
