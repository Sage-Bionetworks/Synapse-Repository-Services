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
import org.sagebionetworks.repo.model.table.AsynchUploadJobBody;
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
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status.getJobId());
		assertNotNull(status.getEtag());
		assertNotNull(status.getChangedOn());
		assertNotNull(status.getStartedOn());
		assertNull(status.getErrorDetails());
		assertNull(status.getErrorMessage());
		assertNotNull(status.getRuntimeMS());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
		assertEquals(creatorUserGroupId, status.getStartedByUserId());
		assertEquals(body, status.getJobBody());
		
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(status, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testNotFound() throws DatastoreException, NotFoundException{
		asynchJobStatusDao.getJobStatus("-99");
	}
	
	@Test
	public void testUpdateProgress() throws DatastoreException, NotFoundException{
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		String newEtag = asynchJobStatusDao.updateJobProgress(status.getJobId(), 0L, 1000L, "A MESSAGE");
		assertNotNull(newEtag);
		assertFalse("The etag must change when the progress changes",startEtag.equals(newEtag));
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals("A MESSAGE", clone.getProgressMessage());
		assertEquals(newEtag, clone.getEtag());
		assertEquals(body, status.getJobBody());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
	}
	
	@Test
	public void testUpdateProgressTooBig() throws DatastoreException, NotFoundException{
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
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
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals(new Long(0), clone.getProgressCurrent());
		assertEquals(new Long(1000), clone.getProgressTotal());
		assertEquals(tooBig.substring(0,  DBOAsynchJobStatus.MAX_MESSAGE_CHARS-1), clone.getProgressMessage());
		assertEquals(body, status.getJobBody());
		assertEquals(newEtag, clone.getEtag());
	}
	
	@Test
	public void testSetFailed() throws DatastoreException, NotFoundException{
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String startEtag = status.getEtag();
		// update the progress
		Throwable error = new Throwable("something when wrong", new IllegalArgumentException("This is bad"));
		String newEtag = asynchJobStatusDao.setJobFailed(status.getJobId(), error);
		assertNotNull(newEtag);
		assertFalse("The etag must change when the status changes",startEtag.equals(newEtag));
		// Get the status
		AsynchronousJobStatus clone = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertEquals("something when wrong", clone.getErrorMessage());
		assertEquals(AsynchJobState.FAILED, clone.getJobState());
		System.out.println(clone.getErrorDetails());
		assertNotNull(clone.getErrorDetails());
		assertTrue(clone.getErrorDetails().contains("This is bad"));
		assertEquals(newEtag, clone.getEtag());
		assertEquals(body, status.getJobBody());
		assertEquals(AsynchJobState.PROCESSING, status.getJobState());
	}
	
	@Test
	public void testSetComplete() throws DatastoreException, NotFoundException, InterruptedException{
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn456");
		body.setUploadFileHandleId("123");
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(creatorUserGroupId, body);
		assertNotNull(status);
		assertNotNull(status.getEtag());
		String previousEtag = status.getEtag();
		// Update the progress
		previousEtag = asynchJobStatusDao.updateJobProgress(status.getJobId(), 10L, 100L, "Made some progress");
		// Now set it complete
		// Make sure at at least some time has passed before me set it complete
		Thread.sleep(10);
		String newEtag = asynchJobStatusDao.setComplete(status.getJobId(), body);
		assertNotNull(newEtag);
		assertFalse(previousEtag.equals(newEtag));
		AsynchronousJobStatus result = asynchJobStatusDao.getJobStatus(status.getJobId());
		assertNotNull(result);
		assertNotNull(result.getEtag());
		assertFalse(previousEtag.equals(result.getEtag()));
		assertTrue(result.getRuntimeMS() >= 10);
		assertNull(result.getErrorDetails());
		assertNull(result.getErrorMessage());
		assertEquals(new Long(100l), result.getProgressCurrent());
		assertEquals(new Long(100l), result.getProgressTotal());
		assertEquals(AsynchJobState.COMPLETE, result.getJobState());
		assertNotNull(result.getChangedOn());
		assertNotNull(result.getStartedOn());
		assertTrue(result.getChangedOn().getTime() > result.getStartedOn().getTime());
		assertEquals(body, result.getJobBody());
	}
	
}
