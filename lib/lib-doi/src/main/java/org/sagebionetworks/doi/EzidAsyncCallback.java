package org.sagebionetworks.doi;

/**
 * Callback handle for the asynchronous client.
 */
public interface EzidAsyncCallback {

	/** When the execution is successful. */
	void onSuccess(EzidMetadata metadata);

	/** When the execution fails with an error. */
	void onError(EzidMetadata metadata, Exception e);
}
