package org.sagebionetworks.repo.web;


import org.sagebionetworks.repo.model.ResponseData;

/**
 * Abstraction for a listener to response accessRecord object.
 * @author John
 *
 */
public interface AccessResponseDataListener {

	/**
	 * Set the ResponseData of the returned object.
	 * @param responseData
	 */
	public void setResponseData(ResponseData responseData);
	
}
