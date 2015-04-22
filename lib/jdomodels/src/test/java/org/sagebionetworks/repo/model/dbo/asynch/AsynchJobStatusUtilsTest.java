package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;

public class AsynchJobStatusUtilsTest {
	
	@Test
	public void testUploadRoundTrip(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setChangedOn(new Date(1));
		status.setJobId("123");
		status.setJobState(AsynchJobState.PROCESSING);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		// request
		UploadToTableRequest requestBody = new UploadToTableRequest();
		requestBody.setTableId("syn123");
		requestBody.setUploadFileHandleId("55555");
		status.setRequestBody(requestBody);
		// response
		UploadToTableResult responseBody = new UploadToTableResult();
		responseBody.setRowsProcessed(1001L);
		
		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchronousJobStatus clone = AsynchJobStatusUtils.createDTOFromDBO(dbo);
		assertEquals(status, clone);
	}
	
	@Test
	public void testUploadRoundTripNullReponse(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setChangedOn(new Date(1));
		status.setJobId("123");
		status.setJobState(AsynchJobState.PROCESSING);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		// request
		UploadToTableRequest requestBody = new UploadToTableRequest();
		requestBody.setTableId("syn123");
		requestBody.setUploadFileHandleId("55555");
		status.setRequestBody(requestBody);
		
		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchronousJobStatus clone = AsynchJobStatusUtils.createDTOFromDBO(dbo);
		assertEquals(status, clone);
	}
	
	@Test
	public void testTruncateMessageStringIfNeededNull(){
		assertNull(AsynchJobStatusUtils.truncateMessageStringIfNeeded(null));
	}
	
	@Test
	public void testTruncateMessageStringIfNeededUnder(){
		assertEquals("under", AsynchJobStatusUtils.truncateMessageStringIfNeeded("under"));
	}
	
	@Test
	public void testTruncateMessageStringIfNeededOver(){
		char[] chars = new char[DBOAsynchJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		String truncate = AsynchJobStatusUtils.truncateMessageStringIfNeeded(tooBig);
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, truncate.length());
	}
	
	@Test
	public void testRoundTripTruncate(){
		char[] chars = new char[DBOAsynchJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setChangedOn(new Date(1));
		status.setErrorDetails("error details");
		status.setErrorMessage(tooBig);
		status.setJobId("123");
		status.setEtag("etag1111");
		status.setJobState(AsynchJobState.COMPLETE);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setProgressMessage(tooBig);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setRuntimeMS(333L);
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn123");
		body.setUploadFileHandleId("55555");
		status.setRequestBody(body);
		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchronousJobStatus clone = AsynchJobStatusUtils.createDTOFromDBO(dbo);
		// Should be truncated
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, clone.getProgressMessage().length());
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, clone.getErrorMessage().length());
	}

}
