package org.sagebionetworks.file.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class AddFilesToDownloadListWorker implements AsyncJobRunner<AddFileToDownloadListRequest, AddFileToDownloadListResponse> {
	
	public static final String MUST_PROVIDE_EITHER_FOLDER_ID_OR_QUERY = "Must provide either 'folderId' or 'query'";

	public static final String SET_EITHER_FOLDER_ID_OR_QUERY_BUT_NOT_BOTH = "Set either 'folderId' or 'query' but not both.";
	
	@Autowired
	private BulkDownloadManager bulkDownloadManager;
	
	@Override
	public Class<AddFileToDownloadListRequest> getRequestType() {
		return AddFileToDownloadListRequest.class;
	}
	
	@Override
	public Class<AddFileToDownloadListResponse> getResponseType() {
		return AddFileToDownloadListResponse.class;
	}

	@Override
	public AddFileToDownloadListResponse run(ProgressCallback progressCallback, String jobId, UserInfo user,
			AddFileToDownloadListRequest request, AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		ValidateArgument.required(request, "AddFileToDownloadListRequest");
		if(request.getFolderId() != null && request.getQuery() != null) {
			throw new IllegalArgumentException(SET_EITHER_FOLDER_ID_OR_QUERY_BUT_NOT_BOTH);
		}
		DownloadList resultList;
		if(request.getFolderId() != null) {
			resultList = bulkDownloadManager.addFilesFromFolder(user, request.getFolderId());
		}else if(request.getQuery() != null) {
			resultList = bulkDownloadManager.addFilesFromQuery(progressCallback, user, request.getQuery());
		}else {
			throw new IllegalArgumentException(MUST_PROVIDE_EITHER_FOLDER_ID_OR_QUERY);
		}
		// job complete.
		AddFileToDownloadListResponse response = new AddFileToDownloadListResponse()
			.setDownloadList(resultList);
		
		return response;	
	}

}
