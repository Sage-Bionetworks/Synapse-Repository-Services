package org.sagebionetworks.table.worker;

import org.sagebionetworks.worker.AsyncJobProgressCallback;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;

/**
 * Updates progress as a file is uloaded to S3.
 * 
 * @author jmhill
 *
 */
public class UploadProgressListener implements ProgressListener {

	public static final String MESSAGE_CREATE_CSV_FILE_HANDLE = "Create CSV FileHandle";
	
	AsyncJobProgressCallback progressCallback;
	long startProgress;
	double bytesTransferedSoFar;
	double bytesPerRow;
	long totalProgress;
	
	/**
	 * 
	 * @param progress
	 * @param originatingMessage
	 * @param startProgress
	 * @param bytesPerRow
	 * @param totalProgress
	 * @param asynchJobStatusManager
	 * @param jobId
	 */
	public UploadProgressListener(AsyncJobProgressCallback progressCallback, long startProgress, double bytesPerRow,
			long totalProgress) {
		super();
		this.progressCallback = progressCallback;
		this.startProgress = startProgress;
		this.bytesPerRow = bytesPerRow;
		this.totalProgress = totalProgress;
		this.bytesTransferedSoFar = 0;
	}

	@Override
	public void progressChanged(ProgressEvent progressEvent) {
		// increment the bytes transfered.
		bytesTransferedSoFar += progressEvent.getBytesTransferred();
		long currentProgress;
		if(bytesTransferedSoFar == 0){
			currentProgress = startProgress;
		}else{
			currentProgress = (long) (startProgress + (bytesTransferedSoFar/bytesPerRow));
		}
		progressCallback.updateProgress(MESSAGE_CREATE_CSV_FILE_HANDLE, currentProgress, totalProgress);
	}

}
