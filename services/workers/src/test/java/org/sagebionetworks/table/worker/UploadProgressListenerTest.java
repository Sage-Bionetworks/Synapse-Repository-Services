package org.sagebionetworks.table.worker;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.worker.AsyncJobProgressCallback;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;

@ExtendWith(MockitoExtension.class)
public class UploadProgressListenerTest {

	@Mock
	private AsyncJobProgressCallback mockCallback;
	
	@Test
	public void testProgress(){
		long rowCount = 100;
		long totalProgress = rowCount*2;
		// Start at the half way mark
		long startPrgoress = totalProgress/2;
		// Assume ten bytes per row.
		long bytesPerRow = 10;
		
		UploadProgressListener listener = new UploadProgressListener(mockCallback, startPrgoress, bytesPerRow, totalProgress);
		// start
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT, 0));
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT,10));
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT,20));
		listener.progressChanged(new ProgressEvent(ProgressEventType.RESPONSE_BYTE_TRANSFER_EVENT,1));
		
		verify(mockCallback, times(1)).updateProgress(UploadProgressListener.MESSAGE_CREATE_CSV_FILE_HANDLE, startPrgoress, totalProgress);
		verify(mockCallback, times(1)).updateProgress(UploadProgressListener.MESSAGE_CREATE_CSV_FILE_HANDLE, startPrgoress+1, totalProgress);
		verify(mockCallback, times(2)).updateProgress(UploadProgressListener.MESSAGE_CREATE_CSV_FILE_HANDLE, startPrgoress+3, totalProgress);
	}
}
