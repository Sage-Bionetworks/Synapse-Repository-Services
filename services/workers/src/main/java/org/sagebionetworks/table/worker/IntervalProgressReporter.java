package org.sagebionetworks.table.worker;

import org.apache.commons.io.input.CountingInputStream;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;

/**
 * This progress reporter will report progress at a given time interval. The
 * progress total will always be the total number of bytes in the file. The
 * progress current will be the number of bytes that have been read from the
 * passed counting stream.
 * 
 * @author jmhill
 * 
 */
public class IntervalProgressReporter implements ProgressReporter {

	private String jobId;
	private long totalSizeBytes;
	private CountingInputStream countingInputStream;
	private AsynchJobStatusManager asynchJobStatusManager;
	private long lastReportTime;
	long progressIntervalMS;
	private int rowNumber;

	/**
	 * Create a new object for each use.
	 * 
	 * @param jobId
	 *            The ID of the job to report progress on.
	 * @param totalSizeBytes
	 *            The number of bytes in the file that is being read. This value
	 *            will be used as progressTotal.
	 * @param countingInputStream
	 *            A input stream that counts the bytes as they are read. The
	 *            CountingInputStream.getByteCount() will be used for
	 *            progressCurrent
	 * @param asynchJobStatusManager The manger used to actually report the progress
	 * @param progressIntervalMS The number of milliseconds that must elapse between reports.
	 */
	public IntervalProgressReporter(String jobId, long totalSizeBytes,
			CountingInputStream countingInputStream,
			AsynchJobStatusManager asynchJobStatusManager,
			long progressIntervalMS) {
		this.jobId = jobId;
		this.totalSizeBytes = totalSizeBytes;
		this.countingInputStream = countingInputStream;
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.progressIntervalMS = progressIntervalMS;
		this.lastReportTime = System.currentTimeMillis();
	}

	@Override
	public void tryReportProgress(int rowNumber) {
		this.rowNumber = rowNumber;
		// Check to see if an interval has elapsed.
		if ((System.currentTimeMillis() - lastReportTime) > this.progressIntervalMS) {
			// It is time to actually send the progress
			asynchJobStatusManager.updateJobProgress(jobId,
					countingInputStream.getByteCount(), totalSizeBytes,
					"Processed: " + rowNumber + " rows");
			// Rest the timer
			lastReportTime = System.currentTimeMillis();
		}
	}

	@Override
	public int getRowNumber() {
		return rowNumber;
	}

}
