package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class AddFilesToDownloadListWorkerTest {
	
	@Mock
	private BulkDownloadManager mockBulkDownloadManager;
	
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	
	@InjectMocks
	private AddFilesToDownloadListWorker worker;
	
	private UserInfo user;
	
	private AddFileToDownloadListRequest addFolderRequest;
	private AsynchronousJobStatus addFolderJobStatus;
	private DownloadList addFolderDownloadList;
	private AddFileToDownloadListRequest addQueryRequest;
	private DownloadList addQueryDownloadList;
	private Query query;
	private String jobId;
	
	@BeforeEach
	public void before() throws Exception {
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 567L);
		
		// add folder
		addFolderRequest = new AddFileToDownloadListRequest();
		addFolderRequest.setFolderId("syn123");
		addFolderJobStatus = new AsynchronousJobStatus();
		addFolderJobStatus.setJobId("9999");
		addFolderJobStatus.setRequestBody(addFolderRequest);
		addFolderJobStatus.setStartedByUserId(user.getId());
	
		addFolderDownloadList = new DownloadList();
		addFolderDownloadList.setEtag("addFolderList");
		addFolderDownloadList.setOwnerId(user.getId().toString());

		
		// add query
		addQueryRequest = new AddFileToDownloadListRequest();
		query = new Query();
		query.setSql("select * from syn123");
		addQueryRequest.setQuery(query);

		addQueryDownloadList = new DownloadList();
		addQueryDownloadList.setEtag("addQueryList");
		addQueryDownloadList.setOwnerId(user.getId().toString());
	}
	
	@Test
	public void testRunAddFolder() throws RecoverableMessageException, Exception {
		when(mockBulkDownloadManager.addFilesFromFolder(user, addFolderRequest.getFolderId())).thenReturn(addFolderDownloadList);
		
		AddFileToDownloadListResponse expectedResponse = new AddFileToDownloadListResponse();
		expectedResponse.setDownloadList(addFolderDownloadList);
		
		// call under test
		AddFileToDownloadListResponse result = worker.run(jobId, user, addFolderRequest, mockJobCallback);
		
		assertEquals(expectedResponse, result);
		
		verify(mockBulkDownloadManager).addFilesFromFolder(user, addFolderRequest.getFolderId());
		verify(mockBulkDownloadManager, never()).addFilesFromQuery(any(), any(UserInfo.class), any(Query.class));
		
	}
	
	@Test
	public void testRunAddQuery() throws RecoverableMessageException, Exception {
		when(mockBulkDownloadManager.addFilesFromQuery(mockJobCallback, user, query)).thenReturn(addQueryDownloadList);
		
		AddFileToDownloadListResponse expectedResponse = new AddFileToDownloadListResponse();
		expectedResponse.setDownloadList(addQueryDownloadList);
		
		// call under test
		AddFileToDownloadListResponse result = worker.run(jobId, user, addQueryRequest, mockJobCallback);
		
		assertEquals(expectedResponse, result);
		
		verify(mockBulkDownloadManager, never()).addFilesFromFolder(any(UserInfo.class), any(String.class));
		verify(mockBulkDownloadManager).addFilesFromQuery(mockJobCallback, user, addQueryRequest.getQuery());
	}
	
	@Test
	public void testRunBothFolderAndQuery() throws RecoverableMessageException, Exception {
		// the folder and query should not be set
		addQueryRequest.setFolderId("syn123");
		addQueryRequest.setQuery(new Query());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			worker.run(jobId, user, addQueryRequest, mockJobCallback);
		}).getMessage();
		
		assertEquals(AddFilesToDownloadListWorker.SET_EITHER_FOLDER_ID_OR_QUERY_BUT_NOT_BOTH, result);
				
	}
	
	@Test
	public void testRunBothFolderAndQueryNull() throws RecoverableMessageException, Exception {
		// the folder and query should not be set
		addQueryRequest.setFolderId(null);
		addQueryRequest.setQuery(null);
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			worker.run(jobId, user, addQueryRequest, mockJobCallback);
		}).getMessage();
		
		assertEquals(AddFilesToDownloadListWorker.MUST_PROVIDE_EITHER_FOLDER_ID_OR_QUERY, result);
	}
	
	@Test
	public void testRunRecoverableException() throws RecoverableMessageException, Exception {
		// setup recoverable exception
		when(mockBulkDownloadManager.addFilesFromQuery(any(), any(UserInfo.class), any(Query.class))).thenThrow(new RecoverableMessageException());
		// call under test
		assertThrows(RecoverableMessageException.class, ()->{
			// call under test
			worker.run(jobId, user, addQueryRequest, mockJobCallback);
		});
	}
	
	@Test
	public void testRunNonRecoverableException() throws RecoverableMessageException, Exception {
		IllegalArgumentException exception = new IllegalArgumentException("not allowed");
		when(mockBulkDownloadManager.addFilesFromQuery(any(), any(UserInfo.class), any(Query.class))).thenThrow(exception);
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			worker.run(jobId, user, addQueryRequest, mockJobCallback);
		});
		
		assertEquals(exception, result);

	}
	
}
