package org.sagebionetworks.repo.model.dbo.file;

import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;

public enum MultiPartRequestType {
	
	UPLOAD(MultipartUploadRequest.class), 
	COPY(MultipartUploadCopyRequest.class);
	
	private Class<? extends MultipartRequest> requestType;
	
	private MultiPartRequestType(Class<? extends MultipartRequest> requestType) {
		this.requestType = requestType;
	}
	
	public Class<? extends MultipartRequest> getRequestType() {
		return requestType;
	}

}
