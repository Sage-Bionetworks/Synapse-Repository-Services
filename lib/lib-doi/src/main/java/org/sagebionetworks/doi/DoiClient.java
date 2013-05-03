package org.sagebionetworks.doi;

/**
 * Client for DOIs.
 */
public interface DoiClient {

	/**
	 * Gets the DOI metadata given the DOI string.
	 */
	EzidDoi get(EzidDoi ezidDoi);

	/**
	 * Creates a new DOI from the supplied data.
	 */
	void create(EzidDoi ezidDoi);

	/**
	 * Updates with the DOI with the supplied data.
	 */
	void update(EzidDoi ezidDoi);
}
