package org.sagebionetworks.repo.model.migration;

/**
 * Simple abstraction for a bucket of data.
 * @author John
 *
 * @param <T>
 */
public interface Bucket <T> {

	/**
	 * Append an object
	 * @param toAppend
	 */
	public void append(T toAppend);
}
