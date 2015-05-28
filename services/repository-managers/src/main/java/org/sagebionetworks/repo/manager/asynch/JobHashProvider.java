package org.sagebionetworks.repo.manager.asynch;

import org.sagebionetworks.repo.model.asynch.CacheableRequestBody;

/**
 * Provides a hash for a given job.
 * 
 *
 */
public interface JobHashProvider {

	/**
	 * Generate a hash for the given given request body and user ID.
	 * Note: The returned hash is the MD5 of the following:
	 * <body_json_> + <object_etag>
	 * @param body
	 * @return
	 */
	public String getJobHash(CacheableRequestBody body);
	
}
