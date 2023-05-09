package org.sagebionetworks.report.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.report.StorageReportManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.util.Clock;


@ExtendWith(MockitoExtension.class)
public class StorageReportCSVDownloadWorkerTest {
	
	public static final long MAX_WAIT_MS = 1000 * 60;

	@Mock
	private StorageReportManager mockStorageReportManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private Clock mockClock;
	@Mock
	private TableExceptionTranslator mockTableExceptionTranslator;

	@InjectMocks
	private StorageReportCSVDownloadWorker storageReportWorker;
	
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	
	DownloadStorageReportRequest request;
	UserInfo adminUser;
	Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	S3FileHandle resultS3FileHandle;
	String resultS3FileHandleId = "999";
	String jobId;

	@BeforeEach
	public void before() throws Exception {
		request = new DownloadStorageReportRequest();
		request.setReportType(StorageReportType.ALL_PROJECTS);
		adminUser = new UserInfo(true);
		adminUser.setId(adminUserId);

		resultS3FileHandle = new S3FileHandle();
		resultS3FileHandle.setId(resultS3FileHandleId);
		jobId = "jobId";
	}

	@Test
	public void testSuccess() throws Exception {

		S3FileHandle resultS3FileHandle = new S3FileHandle();
		resultS3FileHandle.setId(resultS3FileHandleId);
		
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(resultS3FileHandle);
		
		DownloadStorageReportResponse expected = new DownloadStorageReportResponse().setResultsFileHandleId(resultS3FileHandleId);
		
		// Call under test
		DownloadStorageReportResponse response = storageReportWorker.run(jobId, adminUser, request, mockJobCallback);
		
		assertEquals(expected, response);

		// Verify that the data is requested from the manager
		verify(mockStorageReportManager).writeStorageReport(any(), any(), any());

		// Verify that a file was uploaded with the file handle manager
		verify(mockFileHandleManager).uploadLocalFile(any());
	}

	@Test
	public void testFailure() throws Exception {
		IllegalArgumentException ex = new IllegalArgumentException();
		
		doThrow(ex).when(mockStorageReportManager).writeStorageReport(any(), any(), any());
		
		RuntimeException translatedException = new RuntimeException();
		
		when(mockTableExceptionTranslator.translateException(any(IllegalArgumentException.class))).thenReturn(translatedException);

		RuntimeException result = assertThrows(RuntimeException.class, () -> {			
			// Call under test
			storageReportWorker.run(jobId, adminUser, request, mockJobCallback);
		});
		
		assertEquals(translatedException, result);
		
		verify(mockTableExceptionTranslator).translateException(ex);
	}
}
