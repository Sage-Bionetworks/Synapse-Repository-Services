package org.sagebionetworks.migration.worker;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class MigrationWorkerTest {
	
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AsyncMigrationRequestProcessor mockRequestProcessor;
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	
	MigrationWorker migrationWorker;
	

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		migrationWorker = new MigrationWorker();
		ReflectionTestUtils.setField(migrationWorker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(migrationWorker, "userManager", mockUserManager);
		ReflectionTestUtils.setField(migrationWorker, "requestProcessor", mockRequestProcessor);
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAsyncMigrationTypeCountRequest() throws Throwable {
		String jobId = "1";
		Message msg = new Message();
		msg.setBody(jobId);
		
		AsynchronousJobStatus expectedJobStatus = new AsynchronousJobStatus();
		expectedJobStatus.setJobId(jobId);
		expectedJobStatus.setStartedByUserId(100L);
		AsyncMigrationTypeCountRequest expectedReq = new AsyncMigrationTypeCountRequest();
		expectedJobStatus.setRequestBody(expectedReq);
		when(mockAsynchJobStatusManager.lookupJobStatus(anyString())).thenReturn(expectedJobStatus);
		
		UserInfo expectedUserInfo = new UserInfo(true);
		expectedUserInfo.setId(100L);
		when(mockUserManager.getUserInfo(eq(100L))).thenReturn(expectedUserInfo);
		
		migrationWorker.run(mockProgressCallback, msg);
		
		verify(mockRequestProcessor).processAsyncMigrationTypeCountRequest(eq(mockProgressCallback), eq(expectedUserInfo), eq(expectedReq), eq(jobId));
		
	}

	@Test
	public void testAsyncMigrationRangeChecksumRequest() throws Throwable {
		String jobId = "1";
		Message msg = new Message();
		msg.setBody(jobId);
		
		AsynchronousJobStatus expectedJobStatus = new AsynchronousJobStatus();
		expectedJobStatus.setJobId(jobId);
		expectedJobStatus.setStartedByUserId(100L);
		AsyncMigrationRangeChecksumRequest expectedReq = new AsyncMigrationRangeChecksumRequest();
		expectedJobStatus.setRequestBody(expectedReq);
		when(mockAsynchJobStatusManager.lookupJobStatus(anyString())).thenReturn(expectedJobStatus);
		
		UserInfo expectedUserInfo = new UserInfo(true);
		expectedUserInfo.setId(100L);
		when(mockUserManager.getUserInfo(eq(100L))).thenReturn(expectedUserInfo);
		
		migrationWorker.run(mockProgressCallback, msg);
		
		verify(mockRequestProcessor).processAsyncMigrationRangeChecksumRequest(eq(mockProgressCallback), eq(expectedUserInfo), eq(expectedReq), eq(jobId));
		
	}
	
	private class AsyncMigrationInvalidRequest implements AsyncMigrationRequest {

		@Override
		public JSONObjectAdapter initializeFromJSONObject(
				JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo)
				throws JSONObjectAdapterException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getJSONSchema() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getConcreteType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setConcreteType(String concreteType) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUnknowAsyncMigrationRequest() throws Throwable {
		String jobId = "1";
		Message msg = new Message();
		msg.setBody(jobId);
		
		AsynchronousJobStatus expectedJobStatus = new AsynchronousJobStatus();
		expectedJobStatus.setJobId(jobId);
		expectedJobStatus.setStartedByUserId(100L);
		AsyncMigrationInvalidRequest expectedReq = new AsyncMigrationInvalidRequest();
		expectedJobStatus.setRequestBody(expectedReq);
		when(mockAsynchJobStatusManager.lookupJobStatus(anyString())).thenReturn(expectedJobStatus);
		
		UserInfo expectedUserInfo = new UserInfo(true);
		expectedUserInfo.setId(100L);
		when(mockUserManager.getUserInfo(eq(100L))).thenReturn(expectedUserInfo);
		
		migrationWorker.processStatus(mockProgressCallback, msg);
	}

}
