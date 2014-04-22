package org.sagebionetworks.table.worker;

import org.apache.commons.io.input.CountingInputStream;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;

/**
 * reports on the progress of a file stream.
 * 
 * @author jmhill
 *
 */
public class ProgressReporterImpl implements ProgressReporter {

	private String jobId;
	private long totalSizeBytes;
	private CountingInputStream countingInputStream;
	private AsynchJobStatusManager asynchJobStatusManager;
	
	
	
	public ProgressReporterImpl(String jobId, long totalSizeBytes,
			CountingInputStream countingInputStream,
			AsynchJobStatusManager asynchJobStatusManager) {
		super();
		this.jobId = jobId;
		this.totalSizeBytes = totalSizeBytes;
		this.countingInputStream = countingInputStream;
		this.asynchJobStatusManager = asynchJobStatusManager;
	}



	@Override
	public void reportProgress(int rowNumber) {
		asynchJobStatusManager.updateJobProgress(jobId, countingInputStream.getByteCount(), totalSizeBytes, "Processed: "+rowNumber+" rows");
	}

}
