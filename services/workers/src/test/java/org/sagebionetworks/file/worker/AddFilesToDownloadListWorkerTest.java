package org.sagebionetworks.file.worker;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.AddFolderToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddQueryToDownloadListRequest;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@RunWith(MockitoJUnitRunner.class)
public class AddFilesToDownloadListWorkerTest {

	@Mock
	AsynchJobStatusManager mockAsynchJobStatusManager;
	
	@Mock
	UserManager mockUserManager;
	
	@Mock
	BulkDownloadManager mockBulkDownloadManager;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@InjectMocks
	AddFilesToDownloadListWorker worker;
	
	UserInfo user;
	
	AddFolderToDownloadListRequest addFolderRequest;
	AsynchronousJobStatus addFolderJobStatus;
	Message addFolderMessage;
	DownloadList addFolderDownloadList;
	
	AddQueryToDownloadListRequest addQueryRequest;
	AsynchronousJobStatus addQueryJobStatus;
	Message addQueryMessage;
	DownloadList addQueryDownloadList;
	
	@Before
	public void before() throws Exception {
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 567L);
		
		when(mockUserManager.getUserInfo(user.getId())).thenReturn(user);
		
		// add folder
		addFolderRequest = new AddFolderToDownloadListRequest();
		addFolderRequest.setFolderId("syn123");
		addFolderJobStatus = new AsynchronousJobStatus();
		addFolderJobStatus.setJobId("9999");
		addFolderJobStatus.setRequestBody(addFolderRequest);
		addFolderJobStatus.setStartedByUserId(user.getId());
		addFolderMessage = MessageUtils.buildMessage(addFolderJobStatus);
		when(mockAsynchJobStatusManager.lookupJobStatus(addFolderMessage.getBody())).thenReturn(addFolderJobStatus);
		
		addFolderDownloadList = new DownloadList();
		addFolderDownloadList.setEtag("addFolderList");
		addFolderDownloadList.setOwnerId(user.getId().toString());
		when(mockBulkDownloadManager.addFilesFromFolder(user, addFolderRequest.getFolderId())).thenReturn(addFolderDownloadList);
		
		// add query
		addQueryRequest = new AddQueryToDownloadListRequest();
		Query query = new Query();
		query.setSql("select * from syn123");
		addQueryRequest.setQuery(query);
		addQueryJobStatus = new AsynchronousJobStatus();
		addQueryJobStatus.setJobId("8888");
		addQueryJobStatus.setRequestBody(addQueryRequest);
		addQueryJobStatus.setStartedByUserId(user.getId());
		addQueryMessage = MessageUtils.buildMessage(addQueryRequest);
		when(mockAsynchJobStatusManager.lookupJobStatus(addQueryMessage.getBody())).thenReturn(addQueryJobStatus);

		addQueryDownloadList = new DownloadList();
		addQueryDownloadList.setEtag("addQueryList");
		addQueryDownloadList.setOwnerId(user.getId().toString());
		when(mockBulkDownloadManager.addFilesFromQuery(user, query)).thenReturn(addQueryDownloadList);
		
	}
	
	@Test
	public void testRunAddFolder() throws RecoverableMessageException, Exception {
		// call under test
		worker.run(mockProgressCallback, addFolderMessage);
		verify(mockUserManager).getUserInfo(user.getId());
		verify(mockBulkDownloadManager).addFilesFromFolder(user, addFolderRequest.getFolderId());
		verify(mockBulkDownloadManager, never()).addFilesFromQuery(any(UserInfo.class), any(Query.class));
		AddFileToDownloadListResponse expectedResponse = new AddFileToDownloadListResponse();
		expectedResponse.setDownloadList(addFolderDownloadList);
		verify(mockAsynchJobStatusManager).setComplete(addFolderJobStatus.getJobId(), expectedResponse);
		verify(mockAsynchJobStatusManager, never()).setJobFailed(any(String.class), any(Throwable.class));
	}
	
	@Test
	public void testRunAddQuery() throws RecoverableMessageException, Exception {
		// call under test
		worker.run(mockProgressCallback, addQueryMessage);
		verify(mockUserManager).getUserInfo(user.getId());
		verify(mockBulkDownloadManager, never()).addFilesFromFolder(any(UserInfo.class), any(String.class));
		verify(mockBulkDownloadManager).addFilesFromQuery(user, addQueryRequest.getQuery());
		AddFileToDownloadListResponse expectedResponse = new AddFileToDownloadListResponse();
		expectedResponse.setDownloadList(addQueryDownloadList);
		verify(mockAsynchJobStatusManager).setComplete(addQueryJobStatus.getJobId(), expectedResponse);
		verify(mockAsynchJobStatusManager, never()).setJobFailed(any(String.class), any(Throwable.class));
	}
	
	@Test
	public void testRunRecoverableException() throws RecoverableMessageException, Exception {
		// setup recoverable exception
		when(mockBulkDownloadManager.addFilesFromQuery(any(UserInfo.class), any(Query.class))).thenThrow(new RecoverableMessageException());
		// call under test
		try {
			worker.run(mockProgressCallback, addQueryMessage);
			fail();
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockAsynchJobStatusManager, never()).setComplete(any(String.class), any(AsynchronousResponseBody.class));
		verify(mockAsynchJobStatusManager, never()).setJobFailed(any(String.class), any(Throwable.class));
	}
	
	@Test
	public void testRunNonRecoverableException() throws RecoverableMessageException, Exception {
		IllegalArgumentException exception = new IllegalArgumentException("not allowed");
		when(mockBulkDownloadManager.addFilesFromQuery(any(UserInfo.class), any(Query.class))).thenThrow(exception);
		// call under test
		worker.run(mockProgressCallback, addQueryMessage);
		verify(mockAsynchJobStatusManager, never()).setComplete(any(String.class), any(AsynchronousResponseBody.class));
		verify(mockAsynchJobStatusManager).setJobFailed(addQueryJobStatus.getJobId(), exception);
	}
	
}
