package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;

public class AsynchJobStatusUtilsTest {
	TableUpdateTransactionRequest requestBody;
	TableUpdateTransactionResponse responseBody;
	AsynchronousJobStatus status;


	@Before
	public void setUp(){
		// request
		requestBody= new TableUpdateTransactionRequest();
		UploadToTableRequest uploadToTableRequest = new UploadToTableRequest();
		uploadToTableRequest.setTableId("syn123");
		uploadToTableRequest.setUploadFileHandleId("55555");
		requestBody.setChanges(Collections.singletonList(uploadToTableRequest));

		// response
		responseBody = new TableUpdateTransactionResponse();
		UploadToTableResult result = new UploadToTableResult();
		result.setRowsProcessed(1001L);
		responseBody.setResults(Collections.singletonList(result));

		status = new AsynchronousJobStatus();
		status.setChangedOn(new Date(1));
		status.setJobId("123");
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setRequestBody(requestBody);

	}

	@Test
	public void testUploadRoundTrip(){
		status.setJobState(AsynchJobState.PROCESSING);
		status.setResponseBody(responseBody);

		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchronousJobStatus clone = AsynchJobStatusUtils.createDTOFromDBO(dbo);
		assertEquals(status, clone);
	}
	
	@Test
	public void testUploadRoundTripNullReponse(){
		status.setJobState(AsynchJobState.PROCESSING);
		status.setResponseBody(null);
		
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
		status.setErrorDetails("error details");
		status.setErrorMessage(tooBig);
		status.setEtag("etag1111");
		status.setJobState(AsynchJobState.COMPLETE);
		status.setProgressMessage(tooBig);
		status.setRuntimeMS(333L);

		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchronousJobStatus clone = AsynchJobStatusUtils.createDTOFromDBO(dbo);
		// Should be truncated
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, clone.getProgressMessage().length());
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, clone.getErrorMessage().length());
	}

}
