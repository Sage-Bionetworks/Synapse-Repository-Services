package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandlePackageManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class BulkFileDownloadWorkerTest {

	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private UserManager mockUserManger;
	@Mock
	private FileHandlePackageManager mockBulkDownloadManager;
	@Mock
	private ProgressCallback mockProgress;
	@InjectMocks
	private BulkFileDownloadWorker worker;

	@Mock
	private Message mockMessage;

	private String jobId;
	private FileHandleAssociation fha1;
	private FileHandleAssociation fha2;
	private BulkFileDownloadRequest requestBody;
	private UserInfo user;
	private AsynchronousJobStatus jobStatus;
	private BulkFileDownloadResponse response;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		// test objects
		fha1 = new FileHandleAssociation();
		fha1.setFileHandleId("1");
		fha1.setAssociateObjectId("123");
		fha1.setAssociateObjectType(FileHandleAssociateType.TableEntity);

		fha2 = new FileHandleAssociation();
		fha2.setFileHandleId("2");
		fha2.setAssociateObjectId("123");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);

		requestBody = new BulkFileDownloadRequest();
		requestBody.setRequestedFiles(Arrays.asList(fha1));

		user = new UserInfo(false, 777L);

		jobId = "9999";
		jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobId(jobId);
		jobStatus.setRequestBody(requestBody);
		jobStatus.setStartedByUserId(user.getId());

		response = new BulkFileDownloadResponse().setResultZipFileHandleId("56789");

	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(any())).thenReturn(jobStatus);
		when(mockUserManger.getUserInfo(any())).thenReturn(user);
		when(mockBulkDownloadManager.buildZip(any(), any())).thenReturn(response);
		
		// call under test
		worker.run(mockProgress, mockMessage);
		
		verify(mockAsynchJobStatusManager).setComplete(jobId, response);
		verify(mockMessage).getBody();
		verify(mockAsynchJobStatusManager).lookupJobStatus(jobId);
		verify(mockUserManger).getUserInfo(user.getId());
		verify(mockBulkDownloadManager).buildZip(user, requestBody);
	}
	
	@Test
	public void testRunWithWrongType() throws RecoverableMessageException, Exception {
		jobStatus.setRequestBody(new AddToDownloadListRequest());
		when(mockMessage.getBody()).thenReturn(jobId);
		when(mockAsynchJobStatusManager.lookupJobStatus(any())).thenReturn(jobStatus);
		
		// call under test
		worker.run(mockProgress, mockMessage);
		
		verify(mockAsynchJobStatusManager, never()).setComplete(any(), any());
		ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
		verify(mockAsynchJobStatusManager).setJobFailed(eq(jobId), captor.capture());
		assertTrue(captor.getValue() instanceof IllegalArgumentException);
		verify(mockMessage).getBody();
		verify(mockAsynchJobStatusManager).lookupJobStatus(jobId);
		verify(mockUserManger, never()).getUserInfo(anyLong());
		verify(mockBulkDownloadManager, never()).buildZip(any(), any());
	}
}
