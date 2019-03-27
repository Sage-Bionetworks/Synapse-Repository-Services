package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.UploadDaemonStatusDao;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.State;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;

/**
 * Tests for the error conditions of this worker.  The happy cases are tested by FileHandleManagerImplAutowireTest.
 * @author John
 *
 */
public class CompleteUploadWorkerTest {
	
	UploadDaemonStatusDao stubUploadDaemonStatusDao;
	ExecutorService mockThreadPool;
	UploadDaemonStatus uploadStatus;
	ChunkedFileToken token;
	CompleteAllChunksRequest cacf;
	MultipartManager mockmulltipartManager;
	String bucket;
	long maxWaitMS;
	String userId;

	@Before
	public void before(){
		mockThreadPool = Mockito.mock(ExecutorService.class);
		mockmulltipartManager = Mockito.mock(MultipartManager.class);
		stubUploadDaemonStatusDao = new StubUploadDeamonStatusDao();
		token = new ChunkedFileToken();
		cacf = new CompleteAllChunksRequest();
		cacf.setChunkedFileToken(token);
		cacf.setChunkNumbers(new LinkedList<Long>());
		cacf.getChunkNumbers().add(1l);
		cacf.getChunkNumbers().add(2l);
		cacf.getChunkNumbers().add(3l);
		bucket = "bucket";
		userId = "123";
		maxWaitMS = 100;
		
		uploadStatus = new UploadDaemonStatus();
		uploadStatus.setState(State.PROCESSING);
		uploadStatus.setStartedBy(userId);
		uploadStatus = stubUploadDaemonStatusDao.create(uploadStatus);
	}
	
	@Test
	public void testTimeout() throws Exception{
		Future<ChunkResult> mockFuture = Mockito.mock(Future.class);
		when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
		when(mockFuture.isDone()).thenReturn(false);
		CompleteUploadWorker cuw = new CompleteUploadWorker(stubUploadDaemonStatusDao, mockThreadPool, uploadStatus, cacf,
				mockmulltipartManager, maxWaitMS, userId);
		assertFalse(cuw.call());
		// The status should be set to failed
		UploadDaemonStatus status = stubUploadDaemonStatusDao.get(uploadStatus.getDaemonId());
		assertNotNull(status);
		assertEquals(State.FAILED, status.getState());
		assertNotNull(status.getErrorMessage());
		assertTrue(status.getErrorMessage().indexOf("Timed out") > -1);
	}
	
	@Test
	public void testCopyFailed() throws Exception{
		Future<ChunkResult> mockFuture = Mockito.mock(Future.class);
		when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
		when(mockFuture.isDone()).thenReturn(true);
		when(mockFuture.get()).thenThrow(new ExecutionException("some kind of error", new IllegalArgumentException("foo")));
		maxWaitMS = 2000;
		CompleteUploadWorker cuw = new CompleteUploadWorker(stubUploadDaemonStatusDao, mockThreadPool, uploadStatus, cacf,
				mockmulltipartManager, maxWaitMS, userId);
		assertFalse(cuw.call());
		// The status should be set to failed
		UploadDaemonStatus status = stubUploadDaemonStatusDao.get(uploadStatus.getDaemonId());
		assertNotNull(status);
		assertEquals(State.FAILED, status.getState());
		assertNotNull(status.getErrorMessage());
		assertTrue(status.getErrorMessage().indexOf("some kind of error") > -1);
	}
}
