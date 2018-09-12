package org.sagebionetworks.file.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManagerImpl;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.AddFolderToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddQueryToDownloadListRequest;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class AddFilesToDownloadListWorker implements MessageDrivenRunner {
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	BulkDownloadManagerImpl bulkDownloadManager;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {

			if (!(status.getRequestBody() instanceof AddFileToDownloadListRequest)) {
				throw new IllegalArgumentException("Unexpected request body: "
						+ status.getRequestBody());
			}
			AddFileToDownloadListRequest request = AsynchJobUtils.extractRequestBody(status, AddFileToDownloadListRequest.class);
			ValidateArgument.required(request, "AddFileToDownloadListRequest");
			ValidateArgument.required(request.getUserId(), "AddFileToDownloadListRequest.userId");
			// Lookup the user.
			UserInfo user = userManager.getUserInfo(Long.parseLong(request.getUserId()));
			DownloadList resultList;;
			if(request instanceof AddFolderToDownloadListRequest) {
				AddFolderToDownloadListRequest addFolder = (AddFolderToDownloadListRequest) request;
				resultList = bulkDownloadManager.addFilesFromFolder(user, addFolder.getFolderId());
			}else if(request instanceof AddQueryToDownloadListRequest) {
				AddQueryToDownloadListRequest addQuery = (AddQueryToDownloadListRequest) request;
				resultList = bulkDownloadManager.addFilesFromQuery(user, addQuery.getQuery());
			}else {
				throw new IllegalArgumentException("Unknown Request type: "+request.getClass().getName());
			}
			// job complete.
			AddFileToDownloadListResponse response = new AddFileToDownloadListResponse();
			response.setDownloadList(resultList);
			asynchJobStatusManager.setComplete(status.getJobId(), response);
		} catch (RecoverableMessageException e) {
			// pass along RecoverableMessageException to retry the job later.
			throw e;
		}catch (Throwable e) {
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}
		
	}

}
