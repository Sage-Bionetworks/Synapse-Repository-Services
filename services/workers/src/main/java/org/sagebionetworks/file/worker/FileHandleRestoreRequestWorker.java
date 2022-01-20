package org.sagebionetworks.file.worker;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleRestoreRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResult;
import org.sagebionetworks.repo.model.file.FileHandleRestoreStatus;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class FileHandleRestoreRequestWorker implements AsyncJobRunner<FileHandleRestoreRequest, FileHandleRestoreResponse> {

	static final int MAX_BATCH_SIZE = 1000;
	private static final Logger LOG = LogManager.getLogger(FileHandleRestoreRequestWorker.class);

	private FileHandleArchivalManager archivalManager;
	
	@Autowired
	public FileHandleRestoreRequestWorker(FileHandleArchivalManager archivalManager) {
		this.archivalManager = archivalManager;
	}
	
	@Override
	public Class<FileHandleRestoreRequest> getRequestType() {
		return FileHandleRestoreRequest.class;
	}
	
	@Override
	public Class<FileHandleRestoreResponse> getResponseType() {
		return FileHandleRestoreResponse.class;
	}
	
	@Override
	public FileHandleRestoreResponse run(String jobId, UserInfo user, FileHandleRestoreRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		ValidateArgument.requiredNotEmpty(request.getFileHandleIds(), "The fileHandleIds list");
		ValidateArgument.requirement(request.getFileHandleIds().size() <= MAX_BATCH_SIZE, "The number of file handles exceed the maximum allowed (Was: " + request.getFileHandleIds().size() + ", Max:" +MAX_BATCH_SIZE + ").");
		
		List<FileHandleRestoreResult> results = new ArrayList<>(request.getFileHandleIds().size());
		
		for (String id : request.getFileHandleIds()) {
			FileHandleRestoreResult result;
			
			try {
				result = archivalManager.restoreFileHandle(user, id);
			} catch (Exception e) {
				LOG.error("Restore request for file handle " + id + " FAILED: " + e.getMessage(), e);
				result = new FileHandleRestoreResult().setFileHandleId(id).setStatus(FileHandleRestoreStatus.FAILED).setStatusMessage(e.getMessage());
			}
			
			results.add(result);
		}
		
		FileHandleRestoreResponse response = new FileHandleRestoreResponse()
				.setRestoreResults(results);
		
		return response;
	}

}
