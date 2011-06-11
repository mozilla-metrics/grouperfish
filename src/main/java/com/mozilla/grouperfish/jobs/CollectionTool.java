package com.mozilla.grouperfish.jobs;

import com.mozilla.grouperfish.model.Collection;


public interface CollectionTool {

	/**
	 * Run tool (on hadoop).
	 *
	 * @param collection
	 *            The collection this job is about.
	 * @param timestamp
	 *            What is considered "the time" of the job, e.g. for a cluster
	 *            rebuild, this becomes the "last rebuild time".
	 * @return A job that can be submitted.
	 */
	public abstract int run(Collection collection, long timestamp) throws Exception;

	/** The name of the tool (not the job name used by hadoop). */
	public abstract String name();

}