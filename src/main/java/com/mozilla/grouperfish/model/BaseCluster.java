package com.mozilla.grouperfish.model;

import java.util.List;

import org.apache.mahout.math.Vector;

/**
 * A stripped down representation of a cluster, without notion of references and
 * collections.
 */
public class BaseCluster {

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

	public BaseCluster(Vector medoid, List<Vector> related, List<Double> similarities) {
		medoid_ = medoid;
		related_ = related;
		similarities_ = similarities;
	}

	public BaseCluster(BaseCluster data) {
		medoid_ = data.medoid_;
		related_ = data.related_;
		similarities_ = data.similarities_;
	}

	private final Vector medoid_;
	private final List<Vector> related_;
	private final List<Double> similarities_;

}
