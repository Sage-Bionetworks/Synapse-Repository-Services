package org.sagebionetworks.migration.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AdminRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.web.NotFoundException;
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
	
	MigrationWorker migrationWorker;
	UserInfo user;
	

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		migrationWorker = new MigrationWorker();
		ReflectionTestUtils.setField(migrationWorker, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(migrationWorker, "userManager", mockUserManager);
		ReflectionTestUtils.setField(migrationWorker, "migrationManager", mockMigrationManager);
		user = new UserInfo(true);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testProcessAsyncMigration() throws Throwable {
		MigrationTypeCount expectedTypeCount = new MigrationTypeCount();
		expectedTypeCount.setCount(100L);
		AsyncMigrationTypeCountRequest mReq = new AsyncMigrationTypeCountRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		when(mockMigrationManager.processAsyncMigrationTypeCountRequest(user, mReq)).thenReturn(expectedTypeCount);
		AsyncMigrationRequest request = new AsyncMigrationRequest();
		request.setAdminRequest(mReq);
		migrationWorker.processAsyncMigrationRequest(mockCallback, user, request, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, mReq);
		AsyncMigrationResponse expectedResp = new AsyncMigrationResponse();
		expectedResp.setAdminResponse(expectedTypeCount);
		verify(mockAsynchJobStatusManager).setComplete("JOBID", expectedResp);
	}
	
	@Test
	public void testProcessAsyncMigrationFailed() throws Throwable {
		MigrationTypeCount expectedTypeCount = new MigrationTypeCount();
		expectedTypeCount.setCount(100L);
		AsyncMigrationTypeCountRequest mReq = new AsyncMigrationTypeCountRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		Exception expectedException = new RuntimeException("Some exception");
		when(mockMigrationManager.processAsyncMigrationTypeCountRequest(user, mReq)).thenThrow(expectedException);
		AsyncMigrationRequest request = new AsyncMigrationRequest();
		request.setAdminRequest(mReq);
		
		try {
			migrationWorker.processAsyncMigrationRequest(mockCallback, user, request, "JOBID");
		} catch (Exception e) {
		}
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setJobFailed("JOBID", expectedException);
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountRequest() throws Throwable {
		AsyncMigrationTypeCountRequest mReq = new AsyncMigrationTypeCountRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processRequest(user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, mReq);
	}

	@Test
	public void testProcessAsyncMigrationTypeCountsRequest() throws Throwable {
		AsyncMigrationTypeCountsRequest mReq = new AsyncMigrationTypeCountsRequest();
		List<MigrationType> types = Arrays.asList(MigrationType.ACCESS_APPROVAL, MigrationType.ACCESS_REQUIREMENT);
		mReq.setTypes(types);

		migrationWorker.processRequest(user, mReq, "JOBID");

		verify(mockMigrationManager).processAsyncMigrationTypeCountsRequest(user, mReq);
	}
	@Test(expected=Exception.class)
	public void testProcessAsyncMigrationTypeCountRequestFailed() throws Throwable {
		AsyncMigrationTypeCountRequest mReq = new AsyncMigrationTypeCountRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		when(mockMigrationManager.processAsyncMigrationTypeCountRequest(user, mReq)).thenThrow(new Exception("AnException"));
		
		migrationWorker.processRequest(user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, mReq);
		verify(mockAsynchJobStatusManager).setJobFailed(eq("JOBID"), any(Exception.class));
	}
	
	@Test
	public void testProcessAsyncMigrationTypeChecksumRequest() throws Throwable {
		AsyncMigrationTypeChecksumRequest mReq = new AsyncMigrationTypeChecksumRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processRequest(user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationTypeChecksumRequest(user, mReq);
	}
	
	@Test
	public void testProcessAsyncMigrationRangeCountRequest() throws Throwable {
		AsyncMigrationRangeChecksumRequest mReq = new AsyncMigrationRangeChecksumRequest();
		mReq.setMigrationType(MigrationType.ACCESS_APPROVAL);
		migrationWorker.processRequest(user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationRangeChecksumRequest(user, mReq);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcessAsyncMigrationInvalidRequest() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AdminRequest mri = Mockito.mock(AdminRequest.class);
	
		migrationWorker.processRequest(userInfo, mri, jobId);
		
	}

	@Test
	public void testProcessRequestBackupRange() throws Exception {
		String jobId = "123";
		BackupTypeRangeRequest request = new BackupTypeRangeRequest();
		// call under test
		migrationWorker.processRequest(user, request, jobId);
		verify(mockMigrationManager).backupRequest(user, request);
	}
	
	@Test
	public void testProcessRequestCalculateOptimalRanges() throws Exception {
		String jobId = "123";
		CalculateOptimalRangeRequest request = new CalculateOptimalRangeRequest();
		// call under test
		migrationWorker.processRequest(user, request, jobId);
		verify(mockMigrationManager).calculateOptimalRanges(user, request);
	}
	
	@Test
	public void  testProcessRequestBatchChecksumRequest() throws DatastoreException, NotFoundException, IOException {
		String jobId = "123";
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		// call under tes
		migrationWorker.processRequest(user, request, jobId);
		verify(mockMigrationManager).calculateBatchChecksums(user, request);
	}
}
