package org.sagebionetworks.table.worker;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.util.Clock;

import com.amazonaws.services.sqs.model.Message;

import au.com.bytecode.opencsv.CSVWriter;

public class ProgressingCSVWriterStreamTest {
	CSVWriter mockWriter;
	ProgressCallback mockProgress;
	Message mockMessage;
	AsynchJobStatusManager mockAsynchJobStatusManager;
	long currentProgress;
	long totalProgress;
	String jobId;
	Clock mockClock;
	ProgressingCSVWriterStream stream;
	
	@Before
	public void before(){
		mockWriter = Mockito.mock(CSVWriter.class);
		mockProgress = Mockito.mock(ProgressCallback.class);
		mockAsynchJobStatusManager = Mockito.mock(AsynchJobStatusManager.class);
		currentProgress = 0L;
		totalProgress = 100;
		jobId = "123";
		mockClock = Mockito.mock(Clock.class);
		when(mockClock.currentTimeMillis()).thenReturn(0L);
		stream = new ProgressingCSVWriterStream(mockWriter, mockProgress, mockMessage, mockAsynchJobStatusManager, currentProgress, totalProgress, jobId, mockClock);
	}
	
	@Test
	public void testWriteNext() throws InterruptedException{
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2001L, 3000L);
		// write one row.
		String[] one = new String[]{"1"};
		stream.writeNext(one);
		verify(mockWriter).writeNext(one);
		verify(mockClock, never()).sleep(anyLong());
		verify(mockAsynchJobStatusManager, never()).updateJobProgress(anyString(), anyLong(),anyLong(), anyString());
		// Now a little over two seconds have elapse sine the start.
		String[] two = new String[]{"2"};
		stream.writeNext(two);
		verify(mockWriter).writeNext(two);
		verify(mockAsynchJobStatusManager).updateJobProgress(anyString(), anyLong(),anyLong(), anyString());
	}

}
