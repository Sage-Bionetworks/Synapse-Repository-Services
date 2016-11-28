package org.sagebionetworks.migration.worker;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.test.util.ReflectionTestUtils;

public class MigrationWorkerTest {
	
	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private MigrationManager mockMigrationManager;
	@Mock
	private ProgressCallback mockCallback;
	
	ExecutorService migrationExecutorService;
	MigrationWorker migrationWorker;
	UserInfo user;
	

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		migrationExecutorService = Executors.newFixedThreadPool(2);
		migrationWorker = new MigrationWorker();
		ReflectionTestUtils.setField(migrationWorker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(migrationWorker, "userManager", mockUserManager);
		ReflectionTestUtils.setField(migrationWorker, "migrationExecutorService", migrationExecutorService);
		ReflectionTestUtils.setField(migrationWorker, "migrationManager", mockMigrationManager);
		user = new UserInfo(true);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountRequest() throws Throwable {
		AsyncMigrationTypeCountRequest mReq = new AsyncMigrationTypeCountRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processAsyncMigrationRequest(mockCallback, user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setComplete(eq("JOBID"), any(AsynchronousResponseBody.class));
	}
	
	@Test(expected=Exception.class)
	public void testProcessAsyncMigrationTypeCountRequestFailed() throws Throwable {
		AsyncMigrationTypeCountRequest mReq = new AsyncMigrationTypeCountRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		when(mockMigrationManager.processAsyncMigrationTypeCountRequest(user, mReq)).thenThrow(new Exception("AnException"));
		
		migrationWorker.processAsyncMigrationRequest(mockCallback, user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setJobFailed(eq("JOBID"), any(Exception.class));
	}
	
	@Test
	public void testProcessAsyncMigrationTypeChecksumRequest() throws Throwable {
		AsyncMigrationTypeChecksumRequest mReq = new AsyncMigrationTypeChecksumRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processAsyncMigrationRequest(mockCallback, user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeChecksumRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setComplete(eq("JOBID"), any(AsynchronousResponseBody.class));
	}
	
	@Test
	public void testProcessAsyncMigrationRangeCountRequest() throws Throwable {
		AsyncMigrationRangeChecksumRequest mReq = new AsyncMigrationRangeChecksumRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processAsyncMigrationRequest(mockCallback, user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationRangeChecksumRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setComplete(eq("JOBID"), any(AsynchronousResponseBody.class));
	}
	
	@Test
	public void testProcessAsyncMigrationRowMetatdaRequest() throws Throwable {
		AsyncMigrationRowMetadataRequest mReq = new AsyncMigrationRowMetadataRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processAsyncMigrationRequest(mockCallback, user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationRowMetadataRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setComplete(eq("JOBID"), any(AsynchronousResponseBody.class));
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
	public void testProcessAsyncMigrationInvalidRequest() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationInvalidRequest mri = new AsyncMigrationInvalidRequest();
		Exception expectedException = new IllegalArgumentException("SomeException");
	
		migrationWorker.processAsyncMigrationRequest(mockCallback, userInfo, mri, jobId);
		
		verifyZeroInteractions(mockMigrationManager);
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, eq(expectedException));
	}
}
