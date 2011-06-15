package com.mozilla.grouperfish.model;

import java.util.List;


public class Cluster implements Model {

	public Cluster(ClusterRef ref,
				   List<DocumentRef> related,
				   List<Double> similarities) {
		ref_ = ref;
		documents_ = related;
		similarities_ = similarities;
	}

	public Cluster(ClusterRef ref,
			   	   List<DocumentRef> related) {
		ref_ = ref;
		documents_ = related;
		final Double one = Double.valueOf(1.0);
		final int n = related.size();
		similarities_ = new java.util.ArrayList<Double>(n);
		for (int i = 0; i < n; ++i) similarities_.add(one);
	}

	public List<DocumentRef> documents() {
		return documents_;
	}

	public List<Double> similarities() {
		return similarities_;
	}

	@Override
	public ClusterRef ref() {
		return ref_;
	}

	private final ClusterRef ref_;
	private final List<DocumentRef> documents_;
	private final List<Double> similarities_;
}
