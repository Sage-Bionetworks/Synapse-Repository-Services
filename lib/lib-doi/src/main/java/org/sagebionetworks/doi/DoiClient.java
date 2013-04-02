package org.sagebionetworks.doi;

/**
 * Client for DOIs.
 */
public interface DoiClient {

	/**
	 * Creates a new DOI from the supplied data.
	 */
	void create(EzidDoi doi);
}
