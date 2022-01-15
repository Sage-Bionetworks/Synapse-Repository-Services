package org.sagebionetworks.table.worker;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.worker.AsyncJobProgressCallback;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class ProgressingCSVWriterStreamTest {
	@Mock
	private CSVWriter mockWriter;
	@Mock
	private Clock mockClock;
	@Mock
	private AsyncJobProgressCallback mockCallback;
	
	private long currentProgress;
	private long totalProgress;
	
	private ProgressingCSVWriterStream stream;
	
	@BeforeEach
	public void before(){
		currentProgress = 0L;
		totalProgress = 100;
		stream = new ProgressingCSVWriterStream(mockWriter, mockCallback, currentProgress, totalProgress, mockClock);
		when(mockClock.currentTimeMillis()).thenReturn(0L);
	}
	
	@Test
	public void testWriteNext() throws InterruptedException{
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2001L, 3000L);
		// write one row.
		String[] one = new String[]{"1"};
		stream.writeNext(one);
		verify(mockWriter).writeNext(one);
		verify(mockClock, never()).sleep(anyLong());
		verifyZeroInteractions(mockCallback);
		// Now a little over two seconds have elapse sine the start.
		String[] two = new String[]{"2"};
		stream.writeNext(two);
		verify(mockWriter).writeNext(two);
		verify(mockCallback).updateProgress("Building the CSV...", currentProgress + 1, totalProgress);
	}

}
