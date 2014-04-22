package org.sagebionetworks.table.worker;

public interface ProgressReporter {
	
	public void reportProgress(int rowNumber);
}
