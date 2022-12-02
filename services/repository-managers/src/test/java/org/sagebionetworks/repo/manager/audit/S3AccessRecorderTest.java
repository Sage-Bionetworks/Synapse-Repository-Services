package org.sagebionetworks.repo.manager.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.audit.AccessRecord;

@ExtendWith(MockitoExtension.class)
public class S3AccessRecorderTest {

	@InjectMocks
	private S3AccessRecorder recorder;
	@Mock
	private AccessRecordManager mockAccessManager;
	@Mock
	private AwsKinesisFirehoseLogger mockAwsKinesisFirehoseLogger;

	
	@Test
	public void testSaveAndFire() throws IOException{
		String fileName = "testFile";
		when(mockAccessManager.saveBatch(anyList())).thenReturn(fileName);
		List<AccessRecord> toTest = AuditTestUtils.createList(5, 100);

		List<KinesisJsonEntityRecord> kinesisJsonEntityRecords = toTest.stream().map( record ->
				new KinesisJsonEntityRecord(record.getTimestamp(), record, record.getStack(), record.getInstance()))
				.collect(Collectors.toList());

		// Add each to the recorder
		for(AccessRecord ar: toTest){
			recorder.save(ar);
		}
		// Now fire the timer
		String resultFileName= recorder.timerFired();
		assertEquals(fileName,resultFileName);
		// Get the saved record and check it
		verify(mockAccessManager).saveBatch(toTest);
		verify(mockAwsKinesisFirehoseLogger).logBatch(S3AccessRecorder.ACCESS_RECORD_STREAM, kinesisJsonEntityRecords);
	}

	@Test
	public void testAwsKinesisFirehoseThrowException() throws IOException{
		String fileName = "testFile";
		when(mockAccessManager.saveBatch(anyList())).thenReturn(fileName);
		List<AccessRecord> toTest = AuditTestUtils.createList(5, 100);
 		doThrow(new IllegalArgumentException("test exception")).when(mockAwsKinesisFirehoseLogger).logBatch(any(),anyList());
		List<KinesisJsonEntityRecord> kinesisJsonEntityRecords = toTest.stream().map( record ->
						new KinesisJsonEntityRecord(record.getTimestamp(), record, record.getStack(), record.getInstance()))
				.collect(Collectors.toList());

		// Add each to the recorder
		for(AccessRecord ar: toTest){
			recorder.save(ar);
		}
		// Now fire the timer
		String resultFileName= recorder.timerFired();
		assertNull(resultFileName);
		// Get the saved record and check it
		verify(mockAccessManager).saveBatch(toTest);
		verify(mockAwsKinesisFirehoseLogger).logBatch(S3AccessRecorder.ACCESS_RECORD_STREAM, kinesisJsonEntityRecords);
	}
}
