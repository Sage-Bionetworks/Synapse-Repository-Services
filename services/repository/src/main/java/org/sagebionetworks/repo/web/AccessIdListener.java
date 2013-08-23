package org.sagebionetworks.repo.web;

/**
 * Abstraction for a listener to Access object IDs.
 * @author John
 *
 */
public interface AccessIdListener {

	/**
	 * Set the ID of the return object.
	 * @param returneObjectId
	 */
	public void setReturnObjectId(String returneObjectId);
	
}
