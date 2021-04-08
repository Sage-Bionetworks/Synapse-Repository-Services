package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;

import com.amazonaws.services.sqs.model.Message;

public interface FileHandleAssociationScannerNotifier {

	String getQueueUrl();
	
	FileHandleAssociationScanRangeRequest fromSqsMessage(Message message);

	void sendScanRequest(FileHandleAssociationScanRangeRequest request, int delay);
	
}
