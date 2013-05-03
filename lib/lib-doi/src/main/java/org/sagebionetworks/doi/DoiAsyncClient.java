package org.sagebionetworks.doi;

public interface DoiAsyncClient {

	/**
	 * Creates a new DOI from the supplied data.
	 */
	void create(EzidDoi ezidDoi, EzidAsyncCallback callback);

	/**
	 * Updates with the DOI with the supplied data.
	 */
	void update(EzidDoi ezidDoi, EzidAsyncCallback callback);
}
