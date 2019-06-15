package org.sagebionetworks.report.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.report.StorageReportManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.report.StorageReportType;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.csv.CSVWriterStream;

import com.amazonaws.services.sqs.model.Message;


@RunWith(MockitoJUnitRunner.class)
public class StorageReportCSVDownloadWorkerTest {
	
	public static final long MAX_WAIT_MS = 1000 * 60;

	@InjectMocks
	private StorageReportCSVDownloadWorker storageReportWorker;

	@Mock
	private UserManager mockUserManager;
	@Mock
	private StorageReportManager mockStorageReportManager;
	@Mock
	private AsynchJobStatusManager mockAsyncMgr;
	@Mock
	private AsynchronousJobStatus mockStatus;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private Clock mockClock;
	@Mock
	private TableExceptionTranslator mockTableExceptionTranslator;

	DownloadStorageReportRequest request;
	UserInfo adminUser;
	Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	S3FileHandle resultS3FileHandle;
	String resultS3FileHandleId = "999";
	private static final Message message = new Message();
	String jobId;

	@Before
	public void before() throws Exception {
		request = new DownloadStorageReportRequest();
		request.setReportType(StorageReportType.ALL_PROJECTS);
		adminUser = new UserInfo(true);
		adminUser.setId(adminUserId);

		resultS3FileHandle = new S3FileHandle();
		resultS3FileHandle.setId(resultS3FileHandleId);
		when(mockFileHandleManager.multipartUploadLocalFile(any(LocalFileUploadRequest.class))).thenReturn(resultS3FileHandle);
		jobId = "jobId";
		when(mockStatus.getJobId()).thenReturn(jobId);
	}

	@Test
	public void testSuccess() throws Exception {
		when(mockAsyncMgr.lookupJobStatus(message.getBody())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(request);
		when(mockStatus.getStartedByUserId()).thenReturn(adminUserId);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(adminUser);

		// Call under test
		storageReportWorker.run(null, message);

		// Verify that the data is requested from the manager
		verify(mockStorageReportManager).writeStorageReport(any(UserInfo.class), any(DownloadStorageReportRequest.class), any(CSVWriterStream.class));

		// Verify that a file was uploaded with the file handle manager
		verify(mockFileHandleManager).multipartUploadLocalFile(any(LocalFileUploadRequest.class));
		verify(mockAsyncMgr).setComplete(eq(jobId), any(DownloadStorageReportResponse.class));
	}

	@Test
	public void testFailure() throws Exception {
		when(mockAsyncMgr.lookupJobStatus(message.getBody())).thenReturn(mockStatus);
		when(mockStatus.getRequestBody()).thenReturn(request);
		when(mockStatus.getStartedByUserId()).thenReturn(adminUserId);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(adminUser);
		doThrow(new IllegalArgumentException()).when(mockStorageReportManager).writeStorageReport(any(UserInfo.class), any(DownloadStorageReportRequest.class), any(CSVWriterStream.class));
		RuntimeException translatedException = new RuntimeException();
		when(mockTableExceptionTranslator.translateException(any(IllegalArgumentException.class))).thenReturn(translatedException);
		storageReportWorker.run(null, message);
		verify(mockTableExceptionTranslator).translateException(any(Throwable.class));
		verify(mockAsyncMgr).setJobFailed(jobId, translatedException);
	}
}
