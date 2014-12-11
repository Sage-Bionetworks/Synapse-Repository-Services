package org.sagebionetworks.util;

public interface ProgressCallback<T> {
	public void progressMade(T message);
}
