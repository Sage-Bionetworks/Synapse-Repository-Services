package org.sagebionetworks.migration.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AdminRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.BatchChecksumResponse;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeResponse;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.worker.AsyncJobProgressCallback;

@ExtendWith(MockitoExtension.class)
public class MigrationWorkerTest {
	
	@Mock
	private MigrationManager mockMigrationManager;
	@InjectMocks
	private MigrationWorker migrationWorker;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private UserInfo user;
	@Mock
	private AsyncMigrationRequest mockRequest;
	
	@Mock
	private MigrationTypeCount mockTypeCount;
	@Mock
	private MigrationTypeCounts mockTypeCounts;
	@Mock
	private MigrationTypeChecksum mockTypeChecksum;
	@Mock
	private MigrationRangeChecksum mockRangeChecksum;
	@Mock
	private BackupTypeResponse mockBackupRange;
	@Mock	
	private CalculateOptimalRangeResponse mockOptimalRange;
	@Mock
	private BatchChecksumResponse mockBatchChecksum;
	
	private String jobId = "123";
	
	@Test
	public void testRunWithMigrationTypeCountRequest() throws Throwable {
		
		when(mockMigrationManager.processAsyncMigrationTypeCountRequest(any(), any())).thenReturn(mockTypeCount);
		
		AsyncMigrationTypeCountRequest adminRequest = new AsyncMigrationTypeCountRequest().setType(MigrationType.ACCESS_APPROVAL.name());
		
		when(mockRequest.getAdminRequest()).thenReturn(adminRequest);
				
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockTypeCount, result.getAdminResponse());
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountRequest(user, adminRequest);
		
	}

	@Test
	public void testRunWithMigrationTypeCountsRequest() throws Throwable {
		
		AsyncMigrationTypeCountsRequest mReq = new AsyncMigrationTypeCountsRequest();
		List<MigrationType> types = Arrays.asList(MigrationType.ACCESS_APPROVAL, MigrationType.ACCESS_REQUIREMENT);
		mReq.setTypes(types);
		
		when(mockRequest.getAdminRequest()).thenReturn(mReq);
		when(mockMigrationManager.processAsyncMigrationTypeCountsRequest(any(), any())).thenReturn(mockTypeCounts);
		
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockTypeCounts, result.getAdminResponse());
		
		verify(mockMigrationManager).processAsyncMigrationTypeCountsRequest(user, mReq);
	}
	
	@Test
	public void testRunWithAsyncMigrationTypeChecksumRequest() throws Throwable {
		AsyncMigrationTypeChecksumRequest mReq = new AsyncMigrationTypeChecksumRequest();
		mReq.setType(MigrationType.ACCESS_APPROVAL.name());
		
		when(mockRequest.getAdminRequest()).thenReturn(mReq);
		when(mockMigrationManager.processAsyncMigrationTypeChecksumRequest(any(), any())).thenReturn(mockTypeChecksum);
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockTypeChecksum, result.getAdminResponse());
		
		verify(mockMigrationManager).processAsyncMigrationTypeChecksumRequest(user, mReq);
	}
	
	@Test
	public void testRunWithAsyncMigrationRangeChecksumRequest() throws Throwable {
		AsyncMigrationRangeChecksumRequest mReq = new AsyncMigrationRangeChecksumRequest();
		mReq.setMigrationType(MigrationType.ACCESS_APPROVAL);
		
		when(mockRequest.getAdminRequest()).thenReturn(mReq);
		when(mockMigrationManager.processAsyncMigrationRangeChecksumRequest(any(), any())).thenReturn(mockRangeChecksum);
		
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockRangeChecksum, result.getAdminResponse());
		
		verify(mockMigrationManager).processAsyncMigrationRangeChecksumRequest(user, mReq);
	}

	@Test
	public void testRunWithAsyncMigrationInvalidRequest() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AdminRequest mri = Mockito.mock(AdminRequest.class);
		
		when(mockRequest.getAdminRequest()).thenReturn(mri);
	
		assertThrows(IllegalArgumentException.class, () -> {			
			migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		});
	}

	@Test
	public void testRunWithRequestBackupRange() throws Exception {
		String jobId = "123";
		BackupTypeRangeRequest request = new BackupTypeRangeRequest();
		
		when(mockRequest.getAdminRequest()).thenReturn(request);
		when(mockMigrationManager.backupRequest(any(), any())).thenReturn(mockBackupRange);
		
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockBackupRange, result.getAdminResponse());
		
		verify(mockMigrationManager).backupRequest(user, request);
	}
	
	@Test
	public void testRunWithRequestCalculateOptimalRanges() throws Exception {
		String jobId = "123";
		CalculateOptimalRangeRequest request = new CalculateOptimalRangeRequest();

		when(mockRequest.getAdminRequest()).thenReturn(request);
		when(mockMigrationManager.calculateOptimalRanges(any(), any())).thenReturn(mockOptimalRange);
		
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockOptimalRange, result.getAdminResponse());
		
		verify(mockMigrationManager).calculateOptimalRanges(user, request);
	}
	
	@Test
	public void  testRunWithRequestBatchChecksumRequest() throws Exception {
		String jobId = "123";
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		
		when(mockRequest.getAdminRequest()).thenReturn(request);
		when(mockMigrationManager.calculateBatchChecksums(any(), any())).thenReturn(mockBatchChecksum);
		
		// Call under test
		AsyncMigrationResponse result = migrationWorker.run(jobId, user, mockRequest, mockJobCallback);
		
		assertEquals(mockBatchChecksum, result.getAdminResponse());
		verify(mockMigrationManager).calculateBatchChecksums(user, request);
	}
}
