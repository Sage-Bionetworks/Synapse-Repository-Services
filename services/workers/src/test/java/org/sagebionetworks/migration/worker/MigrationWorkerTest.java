package org.sagebionetworks.migration.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.migration.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

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
	
	@Test
	public void testProcessAsyncMigrationRowMetatdaRequest() throws Throwable {
		AsyncMigrationRowMetadataRequest mReq = new AsyncMigrationRowMetadataRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		migrationWorker.processRequest(user, mReq, "JOBID");
		
		verify(mockMigrationManager).processAsyncMigrationRowMetadataRequest(user, mReq);
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
	public void testProcessRequestBackupList() throws Exception {
		String jobId = "123";
		BackupTypeListRequest request = new BackupTypeListRequest();
		// call under test
		migrationWorker.processRequest(user, request, jobId);
		verify(mockMigrationManager).backupRequest(user, request);
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
	public void testProcessRequestDelete() throws Exception {
		String jobId = "123";
		DeleteListRequest request = new DeleteListRequest();
		request.setIdsToDelete(Lists.newArrayList(211L));
		// call under test
		migrationWorker.processRequest(user, request, jobId);
		verify(mockMigrationManager).deleteById(user, request);
	}
	
	@Test
	public void testProcessRequestCalculateOptimalRanges() throws Exception {
		String jobId = "123";
		CalculateOptimalRangeRequest request = new CalculateOptimalRangeRequest();
		// call under test
		migrationWorker.processRequest(user, request, jobId);
		verify(mockMigrationManager).calculateOptimalRanges(user, request);
	}
}
