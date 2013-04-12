package org.sagebionetworks.doi;

import org.sagebionetworks.repo.model.doi.Doi;

/**
 * Client for DOIs.
 */
public interface DoiClient {

	/**
	 * Gets the DOI metadata given the DOI string.
	 */
	EzidDoi get(String doi, Doi doiDto);

	/**
	 * Creates a new DOI from the supplied data.
	 */
	void create(EzidDoi doi);

	/**
	 * Updates with the DOI with the supplied data.
	 */
	void update(EzidDoi doi);
}
