package org.sagebionetworks.table.worker;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.util.Clock;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.sqs.model.Message;

public class ProgressingCSVWriterStreamTest {
	CSVWriter mockWriter;
	WorkerProgress mockProgress;
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
		mockProgress = Mockito.mock(WorkerProgress.class);
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
		verify(mockProgress, never()).progressMadeForMessage(any(Message.class));
		verify(mockAsynchJobStatusManager, never()).updateJobProgress(anyString(), anyLong(),anyLong(), anyString());
		// Now a little over two seconds have elapse sine the start.
		String[] two = new String[]{"2"};
		stream.writeNext(two);
		verify(mockWriter).writeNext(two);
		verify(mockClock).sleep(1L);
		verify(mockProgress).progressMadeForMessage(any(Message.class));
		verify(mockAsynchJobStatusManager).updateJobProgress(anyString(), anyLong(),anyLong(), anyString());
	}

}
