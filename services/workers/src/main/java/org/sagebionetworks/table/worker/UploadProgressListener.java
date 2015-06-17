package org.sagebionetworks.table.worker;

import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.sqs.model.Message;

/**
 * Updates progress as a file is uloaded to S3.
 * 
 * @author jmhill
 *
 */
public class UploadProgressListener implements ProgressListener {

	public static final String MESSAGE_CREATE_CSV_FILE_HANDLE = "Create CSV FileHandle";
	
	ProgressCallback<Message> progressCallback;
	Message originatingMessage;
	long startProgress;
	double bytesTransferedSoFar;
	double bytesPerRow;
	long totalProgress;
	AsynchJobStatusManager asynchJobStatusManager;
	String jobId;
	
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
	public UploadProgressListener(ProgressCallback<Message> progressCallback,
			Message originatingMessage, long startProgress, double bytesPerRow,
			long totalProgress, AsynchJobStatusManager asynchJobStatusManager,
			String jobId) {
		super();
		this.progressCallback = progressCallback;
		this.originatingMessage = originatingMessage;
		this.startProgress = startProgress;
		this.bytesPerRow = bytesPerRow;
		this.totalProgress = totalProgress;
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.jobId = jobId;
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
		// It is time to update the progress
		// notify that progress is still being made for this message
		progressCallback.progressMade(originatingMessage);
		// Update the status
		asynchJobStatusManager.updateJobProgress(jobId, currentProgress, totalProgress, MESSAGE_CREATE_CSV_FILE_HANDLE);
	}

}
