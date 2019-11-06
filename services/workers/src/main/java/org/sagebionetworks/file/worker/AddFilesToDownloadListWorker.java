package org.sagebionetworks.file.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class AddFilesToDownloadListWorker implements MessageDrivenRunner {
	
	public static final String MUST_PROVIDE_EITHER_FOLDER_ID_OR_QUERY = "Must provide either 'folderId' or 'query'";

	public static final String SET_EITHER_FOLDER_ID_OR_QUERY_BUT_NOT_BOTH = "Set either 'folderId' or 'query' but not both.";

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	BulkDownloadManager bulkDownloadManager;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		try {
			AddFileToDownloadListRequest request = AsynchJobUtils.extractRequestBody(status, AddFileToDownloadListRequest.class);
			ValidateArgument.required(request, "AddFileToDownloadListRequest");
			if(request.getFolderId() != null && request.getQuery() != null) {
				throw new IllegalArgumentException(SET_EITHER_FOLDER_ID_OR_QUERY_BUT_NOT_BOTH);
			}
			// Lookup the user.
			UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
			DownloadList resultList;;
			if(request.getFolderId() != null) {
				resultList = bulkDownloadManager.addFilesFromFolder(user, request.getFolderId());
			}else if(request.getQuery() != null) {
				resultList = bulkDownloadManager.addFilesFromQuery(user, request.getQuery());
			}else {
				throw new IllegalArgumentException(MUST_PROVIDE_EITHER_FOLDER_ID_OR_QUERY);
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
