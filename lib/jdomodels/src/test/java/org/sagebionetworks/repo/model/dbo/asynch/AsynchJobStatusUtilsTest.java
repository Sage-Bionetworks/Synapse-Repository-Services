package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobStatusUtils;
import org.sagebionetworks.repo.model.table.AsynchUploadJobStatus;

public class AsynchJobStatusUtilsTest {
	
	
	@Test
	public void testUploadRoundTrip(){
		AsynchUploadJobStatus status = new AsynchUploadJobStatus();
		status.setChangedOn(new Date(1));
		status.setJobId("123");
		status.setJobState(AsynchJobState.PROCESSING);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setTableId("syn123");
		status.setUploadFileHandleId("55555");
		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchUploadJobStatus clone = (AsynchUploadJobStatus) AsynchJobStatusUtils.createDTOFromDBO(dbo, AsynchUploadJobStatus.class);
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
		AsynchUploadJobStatus status = new AsynchUploadJobStatus();
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
		status.setTableId("syn123");
		status.setUploadFileHandleId("55555");
		// to DBO
		DBOAsynchJobStatus dbo = AsynchJobStatusUtils.createDBOFromDTO(status);
		AsynchUploadJobStatus clone = (AsynchUploadJobStatus) AsynchJobStatusUtils.createDTOFromDBO(dbo, AsynchUploadJobStatus.class);
		// Should be truncated
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, clone.getProgressMessage().length());
		assertEquals(DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1, clone.getErrorMessage().length());
	}

}
