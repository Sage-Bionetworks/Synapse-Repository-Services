package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.file.S3FileCopyResult;
import org.sagebionetworks.repo.model.file.S3FileCopyResultType;
import org.sagebionetworks.repo.model.file.S3FileCopyResults;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for AsynchJobStatusManagerImpl
 * 
 * @author John
 *
 */
public class AsynchJobStatusManagerImplTest {
	
	AsynchronousJobStatusDAO mockAsynchJobStatusDao;
	AuthorizationManager mockAuthorizationManager;
	StackStatusDao mockStackStatusDao;
	AsynchJobQueuePublisher mockAsynchJobQueuePublisher;
	JobHashProvider mockJobHashProvider;
	ObjectRecordDAO mockObjectRecordDAO;
	UserInfo user = null;
	AsynchJobStatusManager manager;
	String startedJobId;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		// Setup the mocks
		mockAsynchJobStatusDao = Mockito.mock(AsynchronousJobStatusDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockStackStatusDao = Mockito.mock(StackStatusDao.class);
		mockAsynchJobQueuePublisher = Mockito.mock(AsynchJobQueuePublisher.class);
		mockJobHashProvider = Mockito.mock(JobHashProvider.class);
		mockObjectRecordDAO = Mockito.mock(ObjectRecordDAO.class);
		manager = new AsynchJobStatusManagerImpl();
		
		
		ReflectionTestUtils.setField(manager, "asynchJobStatusDao", mockAsynchJobStatusDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "stackStatusDao", mockStackStatusDao);
		ReflectionTestUtils.setField(manager, "asynchJobQueuePublisher", mockAsynchJobQueuePublisher);
		ReflectionTestUtils.setField(manager, "jobHashProvider", mockJobHashProvider);
		ReflectionTestUtils.setField(manager, "objectRecordDAO", mockObjectRecordDAO);
		startedJobId = "99999";
		stub(mockAsynchJobStatusDao.startJob(anyLong(), any(AsynchronousRequestBody.class))).toAnswer(new Answer<AsynchronousJobStatus>() {
			@Override
			public AsynchronousJobStatus answer(InvocationOnMock invocation)
					throws Throwable {
				Long userId = (Long) invocation.getArguments()[0];
				AsynchronousRequestBody body = (AsynchronousRequestBody) invocation.getArguments()[1];
				AsynchronousJobStatus results = null;
				if(userId != null && body != null){
					results = new AsynchronousJobStatus();
					results.setStartedByUserId(userId);
					results.setRequestBody(body);
					results.setJobId(startedJobId);
				}
				return results;
			}
		});
	
		user = new UserInfo(false);
		user.setId(007L);
		
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("8888");
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartJobNulls() throws DatastoreException, NotFoundException{
		manager.startJob(null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartJobBodyNull() throws DatastoreException, NotFoundException{
		manager.startJob(user, null);
	}
	
	@Test
	public void testStartJobBodyUploadHappy() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn123");
		body.setUploadFileHandleId("456");
		AsynchronousJobStatus status = manager.startJob(user, body);
		assertNotNull(status);
		assertEquals(body, status.getRequestBody());
		verify(mockAsynchJobQueuePublisher, times(1)).publishMessage(status);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetJobStatusUnauthorizedException() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(false);
		manager.getJobStatus(user,"999");
	}
	
	@Test(expected = UnauthorizedException.class)
	public void testCancelJobStatusUnauthorizedException() throws DatastoreException, NotFoundException {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(false);
		manager.cancelJob(user, "999");
	}

	@Test
	public void testGetJobStatusHappy() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(true);
		AsynchronousJobStatus status = manager.getJobStatus(user,"999");
		assertNotNull(status);
	}
	
	/**
	 * Should be able to get a completed job while in read-only mode.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testGetJobStatusReadOnlyComplete() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(true);
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("999");
		status.setJobState(AsynchJobState.COMPLETE);
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		AsynchronousJobStatus result = manager.getJobStatus(user,"999");
		assertNotNull(result);
	}
	
	/**
	 * Should be able to get a failed job while in read-only mode.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test
	public void testGetJobStatusReadOnlyFailed() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(true);
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("999");
		status.setJobState(AsynchJobState.FAILED);
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		AsynchronousJobStatus result = manager.getJobStatus(user,"999");
		assertNotNull(result);
	}
	
	/**
	 * Accessing a PROCESSING job while in read-only mode should trigger an error
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=IllegalStateException.class)
	public void testGetJobStatusReadOnlyProcessing() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(true);
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("999");
		status.setJobState(AsynchJobState.PROCESSING);
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		AsynchronousJobStatus result = manager.getJobStatus(user,"999");
		assertNotNull(result);
	}
	
	/**
	 * Accessing a PROCESSING job while in DOWN mode should trigger an error
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=IllegalStateException.class)
	public void testGetJobStatusDownProcessing() throws DatastoreException, NotFoundException{
		when(mockAuthorizationManager.isUserCreatorOrAdmin(any(UserInfo.class), anyString())).thenReturn(true);
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("999");
		status.setJobState(AsynchJobState.PROCESSING);
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		AsynchronousJobStatus result = manager.getJobStatus(user,"999");
		assertNotNull(result);
	}
	
	/**
	 * Cannot update the progress of a job in read-only or down
	 */
	@Test (expected=IllegalStateException.class)
	public void testUpdateProgressReadOnlyMode(){
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		manager.updateJobProgress("123", 0L, 100L, "testing");
	}
	
	/**
	 * Cannot update the progress of a job in read-only or down
	 */
	@Test (expected=IllegalStateException.class)
	public void testUpdateProgressDownMode(){
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		manager.updateJobProgress("123", 0L, 100L, "testing");
	}
	
	@Test
	public void testUpdateProgressHappy() throws DatastoreException, NotFoundException{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		String jobId = "123";
		manager.updateJobProgress(jobId, 0L, 100L, "testing");
	}

	/**
	 * Cannot set complete when in read-only or down
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@Test (expected=IllegalStateException.class)
	public void testSetCompleteReadOnlyMode() throws Exception{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		UploadToTableResult body = new UploadToTableResult();
		body.setRowsProcessed(101L);
		body.setEtag("etag");
		manager.setComplete("456", body);
	}
	
	/**
	 * Cannot set complete when in read-only or down
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException 
	 */
	@Test (expected=IllegalStateException.class)
	public void testSetCompleteDownMode() throws Exception{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		UploadToTableResult body = new UploadToTableResult();
		body.setRowsProcessed(101L);
		body.setEtag("etag");
		manager.setComplete("456", body);
	}
	
	@Test
	public void testSetCompleteHappy() throws Exception{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockAsynchJobStatusDao.setComplete(anyString(), any(AsynchronousResponseBody.class), anyString())).thenReturn("etag");
		UploadToTableResult body = new UploadToTableResult();
		body.setRowsProcessed(101L);
		body.setEtag("etag");
		String result = manager.setComplete("456", body);
		assertEquals("etag", result);
		String requestHash = null;
		verify(mockAsynchJobStatusDao).setComplete("456", body, requestHash);
	}
	
	@Test
	public void testSetCompleteCacheable() throws Exception{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockAsynchJobStatusDao.setComplete(anyString(), any(AsynchronousResponseBody.class), anyString())).thenReturn("etag");
		
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("8888");
		DownloadFromTableRequest requestbody = new DownloadFromTableRequest();
		requestbody.setSql("select * from syn123");
		status.setRequestBody(requestbody);
		String requestHash = "aRequestHash";
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		when(mockJobHashProvider.getJobHash(requestbody)).thenReturn(requestHash);
		
		DownloadFromTableResult resultBody = new DownloadFromTableResult();
		resultBody.setTableId("syn123");
		String result = manager.setComplete("456", resultBody);
		assertEquals("etag", result);
		verify(mockAsynchJobStatusDao).setComplete("456", resultBody, requestHash);
	}
	
	@Test
	public void testSetCompleteS3FileCopyResult() throws Exception{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockAsynchJobStatusDao.setComplete(anyString(), any(AsynchronousResponseBody.class), anyString())).thenReturn("etag");
		
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setStartedByUserId(user.getId());
		status.setJobId("8888");
		S3FileCopyResult s3FileCopyResult = new S3FileCopyResult();
		s3FileCopyResult.setResultKey("downloadFile");
		s3FileCopyResult.setResultBucket("myFavoriteBucket");
		s3FileCopyResult.setFile("fileToDownload");
		s3FileCopyResult.setResultType(S3FileCopyResultType.COPIED);
		S3FileCopyResults responseBody = new S3FileCopyResults();
		responseBody.setResults(Arrays.asList(s3FileCopyResult));
		String requestHash = null;
		when(mockAsynchJobStatusDao.getJobStatus(anyString())).thenReturn(status);
		
		String result = manager.setComplete("456", responseBody);
		assertEquals("etag", result);
		verify(mockAsynchJobStatusDao).setComplete("456", responseBody, requestHash);
		verify(mockObjectRecordDAO).saveBatch(anyList(), Mockito.eq(S3FileCopyResults.class.getSimpleName().toLowerCase()));
	}
	
	/**
	 * Must be able to set a job failed in any mode.  The failure could be that the stack is not in READ_WRITE mode.
	 */
	@Test
	public void testSetFailedAnyMode(){
		for(StatusEnum mode: StatusEnum.values()){
			when(mockStackStatusDao.getCurrentStatus()).thenReturn(mode);
			when(mockAsynchJobStatusDao.setJobFailed(anyString(), any(Throwable.class))).thenReturn("etag");
			String result = manager.setJobFailed("123", new Throwable("Failed"));
			assertEquals("etag", result);
		}
	}
	
	@Test
	public void testStartJobCacheHit(){
		// request
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		// setup hash and etag.
		String bodyHash = "aBodyHash";
		when(mockJobHashProvider.getJobHash(body)).thenReturn(bodyHash);
		// Match request to existing job
		AsynchronousJobStatus existingJob = new AsynchronousJobStatus();
		existingJob.setStartedByUserId(user.getId());
		existingJob.setJobId("123456");
		existingJob.setRequestBody(body);
		existingJob.setJobState(AsynchJobState.COMPLETE);
		when(mockAsynchJobStatusDao.findCompletedJobStatus(bodyHash, user.getId())).thenReturn(Arrays.asList(existingJob));
		// call under test.
		AsynchronousJobStatus status = manager.startJob(user, body);
		// The status should match the exiting job
		assertEquals(existingJob, status);
		// The job should not be started.
		verify(mockAsynchJobStatusDao, never()).startJob(anyLong(), any(AsynchronousRequestBody.class));
	}
	
	@Test
	public void testStartJobMultipleCacheHit(){
		// request
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		
		DownloadFromTableRequest body2 = new DownloadFromTableRequest();
		body2.setEntityId("syn123");
		body2.setSql("select * from syn123 limit 1");
		
		// setup hash and etag.
		String bodyHash = "aBodyHash";
		when(mockJobHashProvider.getJobHash(body)).thenReturn(bodyHash);
		// Match request to existing job
		List<AsynchronousJobStatus> hits = new LinkedList<AsynchronousJobStatus>();
		// First hit is not a match.
		AsynchronousJobStatus hitOne = new AsynchronousJobStatus();
		hitOne.setStartedByUserId(user.getId());
		hitOne.setJobId("123456");
		hitOne.setRequestBody(body2);
		hitOne.setJobState(AsynchJobState.COMPLETE);
		hits.add(hitOne);
		// this one should match.
		AsynchronousJobStatus hitTwo = new AsynchronousJobStatus();
		hitTwo.setStartedByUserId(user.getId());
		hitTwo.setJobId("123456");
		hitTwo.setRequestBody(body);
		hitTwo.setJobState(AsynchJobState.COMPLETE);
		hits.add(hitTwo);
		
		when(mockAsynchJobStatusDao.findCompletedJobStatus(bodyHash, user.getId())).thenReturn(hits);
		// call under test.
		AsynchronousJobStatus status = manager.startJob(user, body);
		// The status should match the exiting job
		assertEquals(hitTwo, status);
		// The job should not be started.
		verify(mockAsynchJobStatusDao, never()).startJob(anyLong(), any(AsynchronousRequestBody.class));
	}
	
	@Test
	public void testStartJobCacheMiss(){
		// request
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		// setup hash and etag.
		String bodyHash = "aBodyHash";
		when(mockJobHashProvider.getJobHash(body)).thenReturn(bodyHash);
		// For this case, no job exists
		List<AsynchronousJobStatus> existingJob = new LinkedList<AsynchronousJobStatus>();
		when(mockAsynchJobStatusDao.findCompletedJobStatus(bodyHash, user.getId())).thenReturn(existingJob);
		// call under test.
		AsynchronousJobStatus status = manager.startJob(user, body);
		assertNotNull(status);
		assertEquals(startedJobId, status.getJobId());
		// The job should be started and published.
		verify(mockAsynchJobStatusDao, times(1)).startJob(anyLong(), any(AsynchronousRequestBody.class));
		verify(mockAsynchJobQueuePublisher, times(1)).publishMessage(status);
	}
	
	/**
	 * A hash is used to lookup an existing job request so we still need to check if request body is equal to the cache hit.
	 */
	@Test
	public void testStartJobCacheHitNotEquals(){
		// request
		DownloadFromTableRequest body = new DownloadFromTableRequest();
		body.setEntityId("syn123");
		body.setSql("select * from syn123");
		// setup hash and etag.
		String bodyHash = "aBodyHash";
		when(mockJobHashProvider.getJobHash(body)).thenReturn(bodyHash);
		// The cached request body does not equal the body for this request. 
		DownloadFromTableRequest cachedBody = new DownloadFromTableRequest();
		cachedBody.setEntityId("syn123");
		cachedBody.setSql("select * from syn123 limit 1");
		AsynchronousJobStatus existingJob = new AsynchronousJobStatus();
		existingJob.setStartedByUserId(user.getId());
		existingJob.setJobId("123456");
		existingJob.setRequestBody(cachedBody);
		// There is a job with this hash but the body does not match the request's body.
		when(mockAsynchJobStatusDao.findCompletedJobStatus(bodyHash, user.getId())).thenReturn(Arrays.asList(existingJob));
		// call under test.
		AsynchronousJobStatus status = manager.startJob(user, body);
		assertNotNull(status);
		assertEquals(startedJobId, status.getJobId());
		// The job should be started and published.
		verify(mockAsynchJobStatusDao, times(1)).startJob(anyLong(), any(AsynchronousRequestBody.class));
		verify(mockAsynchJobQueuePublisher, times(1)).publishMessage(status);
	}
}
