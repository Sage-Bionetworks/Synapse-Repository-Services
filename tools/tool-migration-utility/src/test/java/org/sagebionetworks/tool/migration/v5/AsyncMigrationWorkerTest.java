package org.sagebionetworks.tool.migration.v5;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;

import org.mockito.internal.util.reflection.Whitebox;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AdminRequest;
import org.sagebionetworks.repo.model.migration.AdminResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.tool.progress.BasicProgress;
import org.sagebionetworks.util.Clock;

public class AsyncMigrationWorkerTest {
	
	@Mock
	private SynapseAdminClient mockClient;
	@Mock
	private Clock mockClock;
	@Mock
	private BasicProgress mockProgress;
	@Mock
	private AdminRequest request;
	@Mock
	private AdminResponse response;
	
	private AsyncMigrationWorker worker;
	private AsyncMigrationRequest migReq;
	private AsyncMigrationResponse migResp;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		AsynchronousJobStatus expectedStartStatus = new AsynchronousJobStatus();
		expectedStartStatus.setJobId("jobId");
		migReq = new AsyncMigrationRequest();
		migReq.setAdminRequest(request);
		migResp = new AsyncMigrationResponse();
		migResp.setAdminResponse(response);
		when(mockClient.startAdminAsynchronousJob(migReq)).thenReturn(expectedStartStatus);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test(expected = InterruptedException.class)
	public void testTimeout() throws Exception {
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2500L);

		worker = new AsyncMigrationWorker(mockClient, request, 1000L, mockProgress);
		Whitebox.setInternalState(worker, "clock", mockClock);
		
		// Call under test
		AdminResponse resp = worker.call();

		verify(mockClient, never()).getAsynchronousJobStatus(anyString());
		verify(mockProgress, never()).setCurrent(anyLong());
		verify(mockProgress, never()).setTotal(anyLong());
		verify(mockClock, never()).sleep(anyLong());
	}
	
	@Test(expected = WorkerFailedException.class)
	public void testFailed() throws Exception {
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2500L);
		AsynchronousJobStatus jobStatus1 = new AsynchronousJobStatus();
		jobStatus1.setJobId("jobId");
		jobStatus1.setJobState(AsynchJobState.FAILED);
		when(mockClient.getAdminAsynchronousJobStatus("jobId")).thenReturn(jobStatus1);
		
		worker = new AsyncMigrationWorker(mockClient, request, 10000L, mockProgress);
		Whitebox.setInternalState(worker, "clock", mockClock);
		
		// Call under test
		AdminResponse resp = worker.call();

		verify(mockProgress, never()).setCurrent(anyLong());
		verify(mockProgress, never()).setTotal(anyLong());
		verify(mockClock, never()).sleep(anyLong());
	}
	
	@Test
	public void testComplete() throws Exception {
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2000L);
		AsynchronousJobStatus jobStatus1 = new AsynchronousJobStatus();
		jobStatus1.setJobId("jobId");
		jobStatus1.setProgressCurrent(100L);
		jobStatus1.setProgressTotal(100L);
		jobStatus1.setProgressMessage("Completed...");
		jobStatus1.setJobState(AsynchJobState.COMPLETE);
		jobStatus1.setResponseBody(migResp);
		when(mockClient.getAdminAsynchronousJobStatus("jobId")).thenReturn(jobStatus1);

		worker = new AsyncMigrationWorker(mockClient, request, 3000L, mockProgress);
		Whitebox.setInternalState(worker, "clock", mockClock);
		
		// Call under test
		AdminResponse resp = worker.call();
		
		assertNotNull(resp);

		verify(mockProgress, never()).setCurrent(anyLong());
		verify(mockProgress, never()).setTotal(anyLong());
		verify(mockClock, never()).sleep(anyLong());
	}
	
	@Test
	public void testProcessing() throws Exception {
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2000L, 3000L, 4000L);
		AsynchronousJobStatus jobStatus1 = new AsynchronousJobStatus();
		jobStatus1.setJobId("jobId");
		jobStatus1.setProgressCurrent(50L);
		jobStatus1.setProgressTotal(100L);
		jobStatus1.setProgressMessage("Progressing...");
		jobStatus1.setJobState(AsynchJobState.PROCESSING);
		AsynchronousJobStatus jobStatus2 = new AsynchronousJobStatus();
		jobStatus2.setJobId("jobId");
		jobStatus2.setProgressCurrent(100L);
		jobStatus2.setProgressTotal(100L);
		jobStatus2.setJobState(AsynchJobState.COMPLETE);
		jobStatus2.setResponseBody(migResp);
		when(mockClient.getAdminAsynchronousJobStatus("jobId")).thenReturn(jobStatus1, jobStatus2);
		
		worker = new AsyncMigrationWorker(mockClient, request, 3000L, mockProgress);
		Whitebox.setInternalState(worker, "clock", mockClock);
		
		// Call under test
		AdminResponse resp = worker.call();
		
		assertNotNull(resp);
		
		verify(mockClock, times(1)).sleep(2000L);
	}
	
	@Test
	public void testLoop() throws Exception {
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2000L, 3000L, 4000L, 5000L, 6000L);
		AsynchronousJobStatus jobStatus1 = new AsynchronousJobStatus();
		jobStatus1.setJobId("jobId");
		jobStatus1.setProgressCurrent(40L);
		jobStatus1.setProgressTotal(100L);
		jobStatus1.setProgressMessage("Progressing...");
		jobStatus1.setJobState(AsynchJobState.PROCESSING);
		AsynchronousJobStatus jobStatus2 = new AsynchronousJobStatus();
		jobStatus2.setJobId("jobId");
		jobStatus2.setProgressCurrent(80L);
		jobStatus2.setProgressTotal(100L);
		jobStatus2.setProgressMessage("Progressing...");
		jobStatus2.setJobState(AsynchJobState.PROCESSING);
		AsynchronousJobStatus jobStatus3 = new AsynchronousJobStatus();
		jobStatus3.setJobId("jobId");
		jobStatus3.setProgressCurrent(100L);
		jobStatus3.setProgressTotal(100L);
		jobStatus3.setJobState(AsynchJobState.COMPLETE);
		jobStatus3.setResponseBody(migResp);
		when(mockClient.getAdminAsynchronousJobStatus("jobId")).thenReturn(jobStatus1, jobStatus2, jobStatus3);
		
		worker = new AsyncMigrationWorker(mockClient, request, 3000L, mockProgress);
		Whitebox.setInternalState(worker, "clock", mockClock);
		
		// Call under test
		AdminResponse resp = worker.call();
		
		assertNotNull(resp);
		
		verify(mockClock, times(2)).sleep(2000L);
	}
	
}
