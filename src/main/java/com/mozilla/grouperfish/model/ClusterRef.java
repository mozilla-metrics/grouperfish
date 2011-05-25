package com.mozilla.grouperfish.model;

public class ClusterRef implements Ref<Cluster> {

	@Override
	public Class<Cluster> model() {
		return Cluster.class;
	}

	/** A handle to the collection that was clustered. */
	public CollectionRef ownerRef() {
		return ownerRef_;
	}

	/** The cluster label, a part of the row key. */
	public String label() {
		return label_;
	}

	/**
	 * Time when the full rebuild was started that created this cluster (or onto
	 * which this cluster was added).
	 */
	public long rebuildTs() {
		return rebuildTs_;
	}

	public ClusterRef(CollectionRef ownerRef, long rebuildTs, String label) {
		ownerRef_ = ownerRef;
		rebuildTs_ = rebuildTs;
		label_ = label;
	}

	private final long rebuildTs_;
	private final String label_;
	private final CollectionRef ownerRef_;

}
