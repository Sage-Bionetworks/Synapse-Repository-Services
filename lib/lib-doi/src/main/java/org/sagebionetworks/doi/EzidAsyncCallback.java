package org.sagebionetworks.doi;

/**
 * Callback handle for the asynchronous client.
 */
public interface EzidAsyncCallback {

	/** When the execution is successful. */
	void onSuccess(EzidDoi doi);

	/** When the execution fails with an error. */
	void onError(EzidDoi doi, Exception e);
}
