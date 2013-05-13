package org.sagebionetworks.repo.model.migration;

public interface BucketProvider<T> {
	
	/**
	 * Create a new appender.
	 * @return
	 */
	public Bucket<T> newBucket();

}
