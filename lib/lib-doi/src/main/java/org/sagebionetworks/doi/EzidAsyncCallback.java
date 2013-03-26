package org.sagebionetworks.doi;

public interface EzidAsyncCallback {
	void onError(Exception e);
	void onSuccess(EzidMetadata metadata);
}
