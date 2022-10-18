package org.sagebionetworks.file.worker;

import org.sagebionetworks.repo.manager.file.FileHandlePackageManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BulkFileDownloadWorker implements AsyncJobRunner<BulkFileDownloadRequest, BulkFileDownloadResponse> {

	private FileHandlePackageManager fileHandleSupport;

	@Autowired
	public BulkFileDownloadWorker(FileHandlePackageManager fileHandleSupport) {
		this.fileHandleSupport = fileHandleSupport;
	}
	
	@Override
	public Class<BulkFileDownloadRequest> getRequestType() {
		return BulkFileDownloadRequest.class;
	}
	
	@Override
	public Class<BulkFileDownloadResponse> getResponseType() {
		return BulkFileDownloadResponse.class;
	}
	
	@Override
	public BulkFileDownloadResponse run(String jobId, UserInfo user, BulkFileDownloadRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		// build the zip from the results
		return fileHandleSupport.buildZip(user, request);
	}

}
