package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;

public interface FileHandleAssociationScannerNotifier {

	String getQueueUrl();

	void sendScanRequest(FileHandleAssociationScanRangeRequest request, int delay);
	
}
