package org.sagebionetworks.file.worker;

import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Worker to process the archival request, the worker will only submit a subset of file handle keys to a queue for processing
 */
@Service
public class FileHandleArchivalRequestWorker implements AsyncJobRunner<FileHandleArchivalRequest, FileHandleArchivalResponse> {
	
	private FileHandleArchivalManager archivalManager;

	@Override
	public Class<FileHandleArchivalRequest> getRequestType() {
		return FileHandleArchivalRequest.class;
	}
	
	@Override
	public Class<FileHandleArchivalResponse> getResponseType() {
		return FileHandleArchivalResponse.class;
	}
	
	@Autowired
	public FileHandleArchivalRequestWorker(FileHandleArchivalManager archivalManager) {
		this.archivalManager = archivalManager;
	}

	@Override
	public FileHandleArchivalResponse run(String jobId, UserInfo user, FileHandleArchivalRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		return archivalManager.processFileHandleArchivalRequest(user, request);
	}

}
