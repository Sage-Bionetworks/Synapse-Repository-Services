package org.sagebionetworks.table.worker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.services.sqs.model.Message;

public class UploadProgressListenerTest {

	WorkerProgress mockProgress;
	AsynchJobStatusManager mockAsynchJobStatusManager;
	Message originatingMessage;
	String jobId;
	
	@Before
	public void before(){
		mockProgress = Mockito.mock(WorkerProgress.class);
		mockAsynchJobStatusManager = Mockito.mock(AsynchJobStatusManager.class);
		originatingMessage = new Message().withMessageId("123").withReceiptHandle("456");
		jobId = "999";
	}
	
	@Test
	public void testProgress(){
		long rowCount = 100;
		long totalProgress = rowCount*2;
		// Start at the half way mark
		long startPrgoress = totalProgress/2;
		// Assume ten bytes per row.
		long bytesPerRow = 10;
		// Start making progress
		UploadProgressListener listener = new UploadProgressListener(mockProgress, originatingMessage, startPrgoress, bytesPerRow, totalProgress, mockAsynchJobStatusManager, jobId);
		// start
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT, 0));
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT,10));
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT,20));
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT,1));
		
		verify(mockProgress, times(4)).progressMadeForMessage(originatingMessage);
		verify(mockAsynchJobStatusManager, times(1)).updateJobProgress(jobId, startPrgoress, totalProgress, UploadProgressListener.MESSAGE_CREATE_CSV_FILE_HANDLE);
		verify(mockAsynchJobStatusManager, times(1)).updateJobProgress(jobId, startPrgoress+1, totalProgress, UploadProgressListener.MESSAGE_CREATE_CSV_FILE_HANDLE);
		verify(mockAsynchJobStatusManager, times(2)).updateJobProgress(jobId, startPrgoress+3, totalProgress, UploadProgressListener.MESSAGE_CREATE_CSV_FILE_HANDLE);
	}
}
