package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.AsynchJobState;
import org.sagebionetworks.repo.model.table.AsynchJobType;
import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;

public class AsynchTableJobStatusUtilsTest {
	
	/**
	 * Round trip from the DTO to DBO.
	 */
	@Test
	public void testRoundTrip(){
		AsynchTableJobStatus status = new AsynchTableJobStatus();
		status.setChangedOn(new Date(1));
		status.setDownloadURL("a download url");
		status.setErrorDetails("error details");
		status.setErrorMessage("error message");
		status.setJobId("123");
		status.setEtag("etag1111");
		status.setJobState(AsynchJobState.COMPLETE);
		status.setJobType(AsynchJobType.UPLOAD);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setProgressMessage("progress message");
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setTableId("syn123");
		status.setUploadFileHandleId("55555");
		// to DBO
		DBOAsynchTableJobStatus dbo = AsynchTableJobStatusUtils.createDBOFromDTO(status);
		AsynchTableJobStatus clone = AsynchTableJobStatusUtils.createDTOFromDBO(dbo);
		assertEquals(status, clone);
	}
	
	@Test
	public void testUploadRoundTrip(){
		AsynchTableJobStatus status = new AsynchTableJobStatus();
		status.setChangedOn(new Date(1));
		status.setJobId("123");
		status.setJobState(AsynchJobState.PROCESSING);
		status.setJobType(AsynchJobType.UPLOAD);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setTableId("syn123");
		status.setUploadFileHandleId("55555");
		// to DBO
		DBOAsynchTableJobStatus dbo = AsynchTableJobStatusUtils.createDBOFromDTO(status);
		AsynchTableJobStatus clone = AsynchTableJobStatusUtils.createDTOFromDBO(dbo);
		assertEquals(status, clone);
	}
	
	@Test
	public void testDownloadRoundTrip(){
		AsynchTableJobStatus status = new AsynchTableJobStatus();
		status.setChangedOn(new Date(1));
		status.setDownloadURL("a download url");
		status.setJobId("123");
		status.setJobState(AsynchJobState.PROCESSING);
		status.setJobType(AsynchJobType.DOWNLOAD);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setTableId("syn123");
		// to DBO
		DBOAsynchTableJobStatus dbo = AsynchTableJobStatusUtils.createDBOFromDTO(status);
		AsynchTableJobStatus clone = AsynchTableJobStatusUtils.createDTOFromDBO(dbo);
		assertEquals(status, clone);
	}
	
	@Test
	public void testTruncateMessageStringIfNeededNull(){
		assertNull(AsynchTableJobStatusUtils.truncateMessageStringIfNeeded(null));
	}
	
	@Test
	public void testTruncateMessageStringIfNeededUnder(){
		assertEquals("under", AsynchTableJobStatusUtils.truncateMessageStringIfNeeded("under"));
	}
	
	@Test
	public void testTruncateMessageStringIfNeededOver(){
		char[] chars = new char[DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		String truncate = AsynchTableJobStatusUtils.truncateMessageStringIfNeeded(tooBig);
		assertEquals(DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS-1, truncate.length());
	}
	
	@Test
	public void testRoundTripTruncate(){
		char[] chars = new char[DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		AsynchTableJobStatus status = new AsynchTableJobStatus();
		status.setChangedOn(new Date(1));
		status.setDownloadURL(tooBig);
		status.setErrorDetails("error details");
		status.setErrorMessage(tooBig);
		status.setJobId("123");
		status.setEtag("etag1111");
		status.setJobState(AsynchJobState.COMPLETE);
		status.setJobType(AsynchJobType.UPLOAD);
		status.setProgressCurrent(222L);
		status.setProgressTotal(444L);
		status.setProgressMessage(tooBig);
		status.setStartedByUserId(999L);
		status.setStartedOn(new Date(900000));
		status.setTableId("syn123");
		status.setUploadFileHandleId("55555");
		// to DBO
		DBOAsynchTableJobStatus dbo = AsynchTableJobStatusUtils.createDBOFromDTO(status);
		AsynchTableJobStatus clone = AsynchTableJobStatusUtils.createDTOFromDBO(dbo);
		// All three should be truncated
		assertEquals(DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS-1, clone.getDownloadURL().length());
		assertEquals(DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS-1, clone.getProgressMessage().length());
		assertEquals(DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS-1, clone.getErrorMessage().length());
	}

}
