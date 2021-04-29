package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleUnlinkedRequest;

import com.amazonaws.services.sqs.model.Message;

public interface FileHandleUnlinkedManager {
	
	FileHandleUnlinkedRequest fromSqsMessage(Message message);
	
	void processFileHandleUnlinkRequest(FileHandleUnlinkedRequest request) throws RecoverableException;

}
