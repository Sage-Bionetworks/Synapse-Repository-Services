package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;

import com.amazonaws.services.sqs.model.Message;

public interface FileHandleArchivalManager {
	
	FileHandleArchivalResponse processFileHandleArchivalRequest(UserInfo user, FileHandleArchivalRequest request);
	
	void processFileHandleKeyArchiveRequest(Message request);

}
