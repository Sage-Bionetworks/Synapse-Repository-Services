package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dao.table.AsynchTableJobStatusDAO;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOAsynchTableJobStatus;
import org.sagebionetworks.repo.model.table.AsynchJobState;
import org.sagebionetworks.repo.model.table.AsynchJobType;
import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AsynchTableJobStatusDaoImplTest {
	
	@Autowired
	AsynchTableJobStatusDAO asynchTableJobStatusDao;
	private Long creatorUserGroupId;

	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		asynchTableJobStatusDao.truncateAllAsynchTableJobStatus();
	}
	
	@Test
	public void testUploadCreateGet() throws DatastoreException, NotFoundException{
		String tableId = "syn456";
		Long fileHandleId = 123L;
		AsynchTableJobStatus status = asynchTableJobStatusDao.starteNewUploadJobStatus(creatorUserGroupId, fileHandleId, tableId);
		assertNotNull(status);
		assertNotNull(status.getJobId());
		assertNotNull(status.getEtag());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		assertNull(status.getErrorDetails());
		assertNull(status.getErrorMessage());
		assertEquals(creatorUserGroupId, status.getStartedByUserId());
		assertEquals(tableId, status.getTableId());
		assertEquals(fileHandleId.toString(), status.getUploadFileHandleId());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
		assertEquals(AsynchJobType.UPLOAD, status.getJobType());
		
		AsynchTableJobStatus clone = asynchTableJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(status, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws DatastoreException, NotFoundException{
		asynchTableJobStatusDao.getJobStatus("-99");
	}
	
	@Test
	public void testUpdateProgress() throws DatastoreException, NotFoundException{
		String tableId = "syn456";
		Long fileHandleId = 123L;
		AsynchTableJobStatus status = asynchTableJobStatusDao.starteNewUploadJobStatus(creatorUserGroupId, fileHandleId, tableId);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		String newEtag = asynchTableJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE");
		assertNotNull(newEtag);
		assertFalse("The etag must change when the progress changes",startEtag.equals(newEtag));
		AsynchTableJobStatus clone = asynchTableJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals("A MESSAGE", clone.getProgressMessage());
		assertEquals(newEtag, clone.getEtag());
	}
	
	@Test
	public void testUpdateProgressTooBig() throws DatastoreException, NotFoundException{
		String tableId = "syn456";
		Long fileHandleId = 123L;
		AsynchTableJobStatus status = asynchTableJobStatusDao.starteNewUploadJobStatus(creatorUserGroupId, fileHandleId, tableId);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		char[] chars = new char[DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		String newEtag = asynchTableJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, tooBig);
		assertNotNull(newEtag);
		assertFalse("The etag must change when the progress changes",startEtag.equals(newEtag));
		AsynchTableJobStatus clone = asynchTableJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals(tooBig.substring(0,  DBOAsynchTableJobStatus.MAX_MESSAGE_CHARS-1), clone.getProgressMessage());
		assertEquals(newEtag, clone.getEtag());
	}
	
	@Test
	public void testSetFailed() throws DatastoreException, NotFoundException{
		String tableId = "syn456";
		Long fileHandleId = 123L;
		AsynchTableJobStatus status = asynchTableJobStatusDao.starteNewUploadJobStatus(creatorUserGroupId, fileHandleId, tableId);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		Throwable error = new Throwable("something when wrong", new IllegalArgumentException("This is bad"));
		String newEtag = asynchTableJobStatusDao.setJobFailed(status.getJobId(), error);
		assertNotNull(newEtag);
		assertFalse("The etag must change when the status changes",startEtag.equals(newEtag));
		// Get the status
		AsynchTableJobStatus clone = asynchTableJobStatusDao.getJobStatus(status.getJobId());
		assertEquals("something when wrong", clone.getErrorMessage());
		assertEquals(AsynchJobState.FAILED, clone.getJobState());
		System.out.println(clone.getErrorDetails());
		assertNotNull(clone.getErrorDetails());
		assertTrue(clone.getErrorDetails().contains("This is bad"));
		assertEquals(newEtag, clone.getEtag());
	}
	
}
