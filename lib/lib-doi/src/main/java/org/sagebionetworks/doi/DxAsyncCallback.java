package org.sagebionetworks.doi;

public interface DxAsyncCallback {

	/** When the execution is successful. */
	void onSuccess(EzidDoi ezidDoi);

	/** When the execution fails with an error. */
	void onError(EzidDoi ezidDoi, Exception e);
}
