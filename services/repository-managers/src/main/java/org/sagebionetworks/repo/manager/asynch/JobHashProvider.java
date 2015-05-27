package org.sagebionetworks.repo.manager.asynch;

import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;

/**
 * Provides a hash for a given job.
 * 
 *
 */
public interface JobHashProvider {

	/**
	 * Generate a hash for the given request body.
	 * 
	 * @param body
	 * @return
	 */
	public String getJobHash(AsynchronousRequestBody body);
	
	/**
	 * Get the current etag of the request object.
	 * 
	 * @param body
	 * @return Returns null if the request object does not have an etag, otherwise the current etag of the object.
	 */
	public String getRequestObjectEtag(AsynchronousRequestBody body);
	
}
