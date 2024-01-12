package org.sagebionetworks.repo.manager.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.audit.AccessRecord;

@ExtendWith(MockitoExtension.class)
public class KinesisAccessRecorderTest {

	@InjectMocks
	private KinesisAccessRecorder recorder;
	@Mock
	private AwsKinesisFirehoseLogger mockAwsKinesisFirehoseLogger;

	
	@Test
	public void testSaveAndFire() throws IOException{
		List<AccessRecord> toTest = createList(5, 100);

		List<KinesisJsonEntityRecord<AccessRecord>> kinesisJsonEntityRecords = toTest.stream()
			.map( record -> new KinesisJsonEntityRecord<>(record.getTimestamp(), record, record.getStack(), record.getInstance()))
			.collect(Collectors.toList());

		// Add each to the recorder
		for(AccessRecord ar: toTest){
			recorder.save(ar);
		}
		// Now fire the timer
		recorder.timerFired();
		// Get the saved record and check it
		verify(mockAwsKinesisFirehoseLogger).logBatch(KinesisAccessRecorder.ACCESS_RECORD_STREAM, kinesisJsonEntityRecords);
	}

	@Test
	public void testAwsKinesisFirehoseThrowException() throws IOException{
		List<AccessRecord> toTest = createList(5, 100);
 		
		doThrow(new IllegalArgumentException("test exception")).when(mockAwsKinesisFirehoseLogger).logBatch(any(),anyList());
 		
 		List<KinesisJsonEntityRecord<AccessRecord>> kinesisJsonEntityRecords = toTest.stream()
 				.map( record -> new KinesisJsonEntityRecord<>(record.getTimestamp(), record, record.getStack(), record.getInstance()))
 				.collect(Collectors.toList());

		// Add each to the recorder
		for(AccessRecord ar: toTest){
			recorder.save(ar);
		}
		// Now fire the timer
		recorder.timerFired();
		// Get the saved record and check it
		verify(mockAwsKinesisFirehoseLogger).logBatch(KinesisAccessRecorder.ACCESS_RECORD_STREAM, kinesisJsonEntityRecords);
	}
	
	private static List<AccessRecord> createList(int count, long startTimestamp){
		List<AccessRecord> list = new LinkedList<AccessRecord>();
		for(int i=0; i<count; i++){
			AccessRecord ar = new AccessRecord();
			ar.setUserId((long) i);
			ar.setElapseMS((long) (10*i));
			ar.setTimestamp(startTimestamp+i);
			ar.setMethod(Method.values()[i%4].toString());
			if(i%2 > 0){
				ar.setSuccess(true);
				ar.setResponseStatus(201L);
			}else{
				ar.setSuccess(false);
				ar.setResponseStatus(401L);
			}
			ar.setRequestURL("/url/"+i);
			ar.setSessionId(UUID.randomUUID().toString());
			ar.setHost("localhost:8080");
			ar.setOrigin("http://www.example-social-network.com");
			ar.setUserAgent("The bat-mobile OS");
			ar.setThreadId(Thread.currentThread().getId());
			ar.setVia("1 then two");
			ar.setStack("dev");
			ar.setInstance("test");

			list.add(ar);
		}
		return list;
	}
	
	enum Method{
		GET,POST,PUT,DELETE
	}
}
