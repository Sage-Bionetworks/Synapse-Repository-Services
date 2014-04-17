package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.table.AsynchUploadJobStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AsynchJobStatusDaoImplTest {
	
	@Autowired
	AsynchronousJobStatusDAO asynchJobStatusDao;
	private Long creatorUserGroupId;

	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		asynchJobStatusDao.truncateAllAsynchTableJobStatus();
	}
	
	@Test
	public void testUploadCreateGet() throws DatastoreException, NotFoundException{
		AsynchUploadJobStatus dto = new AsynchUploadJobStatus();
		dto.setTableId("syn456");
		dto.setUploadFileHandleId("123");
		dto.setStartedByUserId(creatorUserGroupId);
		AsynchUploadJobStatus result = asynchJobStatusDao.startJob(dto);
		AsynchUploadJobStatus status = (AsynchUploadJobStatus) result;
		assertNotNull(status.getJobId());
		assertNotNull(status.getEtag());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		assertNull(status.getErrorDetails());
		assertNull(status.getErrorMessage());

		assertEquals(creatorUserGroupId, status.getStartedByUserId());
		assertEquals(dto.getTableId(), status.getTableId());
		assertEquals(dto.getUploadFileHandleId(), status.getUploadFileHandleId());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
		
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId(), AsynchUploadJobStatus.class);
		assertEquals(status, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws DatastoreException, NotFoundException{
		asynchJobStatusDao.getJobStatus("-99", AsynchUploadJobStatus.class);
	}
	
	@Test
	public void testUpdateProgress() throws DatastoreException, NotFoundException{
		AsynchUploadJobStatus dto = new AsynchUploadJobStatus();
		dto.setTableId("syn456");
		dto.setUploadFileHandleId("123");
		dto.setStartedByUserId(creatorUserGroupId);
		AsynchUploadJobStatus status = asynchJobStatusDao.startJob(dto);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		String newEtag = asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE");
		assertNotNull(newEtag);
		assertFalse("The etag must change when the progress changes",startEtag.equals(newEtag));
		AsynchUploadJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId(), AsynchUploadJobStatus.class);
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals("A MESSAGE", clone.getProgressMessage());
		assertEquals(newEtag, clone.getEtag());
		assertEquals(dto.getTableId(), status.getTableId());
		assertEquals(dto.getUploadFileHandleId(), status.getUploadFileHandleId());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
	}
	
	@Test
	public void testUpdateProgressTooBig() throws DatastoreException, NotFoundException{
		AsynchUploadJobStatus dto = new AsynchUploadJobStatus();
		dto.setTableId("syn456");
		dto.setUploadFileHandleId("123");
		dto.setStartedByUserId(creatorUserGroupId);
		AsynchUploadJobStatus status = asynchJobStatusDao.startJob(dto);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		char[] chars = new char[DBOAsynchJobStatus.MAX_MESSAGE_CHARS+1];
		Arrays.fill(chars, '1');
		String tooBig = new String(chars);
		String newEtag = asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, tooBig);
		assertNotNull(newEtag);
		assertFalse("The etag must change when the progress changes",startEtag.equals(newEtag));
		AsynchUploadJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId(), AsynchUploadJobStatus.class);
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals(tooBig.substring(0,  DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1), clone.getProgressMessage());
		assertEquals(newEtag, clone.getEtag());
	}
	
	@Test
	public void testSetFailed() throws DatastoreException, NotFoundException{
		AsynchUploadJobStatus dto = new AsynchUploadJobStatus();
		dto.setTableId("syn456");
		dto.setUploadFileHandleId("123");
		dto.setStartedByUserId(creatorUserGroupId);
		AsynchUploadJobStatus status = asynchJobStatusDao.startJob(dto);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		Throwable error = new Throwable("something when wrong", new IllegalArgumentException("This is bad"));
		String newEtag = asynchJobStatusDao.setJobFailed(status.getJobId(), error);
		assertNotNull(newEtag);
		assertFalse("The etag must change when the status changes",startEtag.equals(newEtag));
		// Get the status
		AsynchUploadJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId(), AsynchUploadJobStatus.class);
		assertEquals("something when wrong", clone.getErrorMessage());
		assertEquals(AsynchJobState.FAILED, clone.getJobState());
		System.out.println(clone.getErrorDetails());
		assertNotNull(clone.getErrorDetails());
		assertTrue(clone.getErrorDetails().contains("This is bad"));
		assertEquals(newEtag, clone.getEtag());
		assertEquals(dto.getTableId(), status.getTableId());
		assertEquals(dto.getUploadFileHandleId(), status.getUploadFileHandleId());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
	}
	
}
