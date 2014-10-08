package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.status.StatusEnum;
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
	UserInfo user = null;
	AsynchJobStatusManager manager;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		// Setup the mocks
		mockAsynchJobStatusDao = Mockito.mock(AsynchronousJobStatusDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockStackStatusDao = Mockito.mock(StackStatusDao.class);
		mockAsynchJobQueuePublisher = Mockito.mock(AsynchJobQueuePublisher.class);
		manager = new AsynchJobStatusManagerImpl();
		
		
		ReflectionTestUtils.setField(manager, "asynchJobStatusDao", mockAsynchJobStatusDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "stackStatusDao", mockStackStatusDao);
		ReflectionTestUtils.setField(manager, "asynchJobQueuePublisher", mockAsynchJobQueuePublisher);
		
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
					results.setJobId("99999");
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
	
	@Test (expected=UnauthorizedException.class)
	public void testStartJobBodyUploadUnauthorizedException() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn123");
		body.setUploadFileHandleId("456");
		when(mockAuthorizationManager.canUserStartJob(user, body)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.startJob(user, body);
	}
	
	@Test
	public void testStartJobBodyUploadHappy() throws DatastoreException, NotFoundException{
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn123");
		body.setUploadFileHandleId("456");
		when(mockAuthorizationManager.canUserStartJob(user, body)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
		when(mockAsynchJobStatusDao.updateJobProgress(anyString(), anyLong(), anyLong(), anyString())).thenReturn("etag");
		String result = manager.updateJobProgress(jobId,  0L, 100L, "testing");
		assertEquals("etag", result);
	}

	/**
	 * Cannot set complete when in read-only or down
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@Test (expected=IllegalStateException.class)
	public void testSetCompleteReadOnlyMode() throws DatastoreException, NotFoundException{
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
	 */
	@Test (expected=IllegalStateException.class)
	public void testSetCompleteDownMode() throws DatastoreException, NotFoundException{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		UploadToTableResult body = new UploadToTableResult();
		body.setRowsProcessed(101L);
		body.setEtag("etag");
		manager.setComplete("456", body);
	}
	
	@Test
	public void testSetCompleteHappy() throws DatastoreException, NotFoundException{
		when(mockStackStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockAsynchJobStatusDao.setComplete(anyString(), any(AsynchronousResponseBody.class))).thenReturn("etag");
		UploadToTableResult body = new UploadToTableResult();
		body.setRowsProcessed(101L);
		body.setEtag("etag");
		String result = manager.setComplete("456", body);
		assertEquals("etag", result);
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
}
