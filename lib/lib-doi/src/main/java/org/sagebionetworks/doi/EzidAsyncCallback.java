package org.sagebionetworks.doi;

/**
 * Callback handle for the asynchronous client.
 */
public interface EzidAsyncCallback {

	/** When the execution is successful. */
	void onSuccess(EzidDoi ezidDoi);

	/** When the execution fails with an error. */
	void onError(EzidDoi ezidDoi, Exception e);
}
