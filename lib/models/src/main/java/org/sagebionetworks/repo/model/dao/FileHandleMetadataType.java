package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public enum FileHandleMetadataType {
	S3(S3FileHandle.class),
	GOOGLE_CLOUD(GoogleCloudFileHandle.class),
	EXTERNAL(ExternalFileHandle.class),
	PROXY(ProxyFileHandle.class),
	EXTERNAL_OBJ_STORE(ExternalObjectStoreFileHandle.class);
	
	private Class<? extends FileHandle> fileClass;
	
	private FileHandleMetadataType(Class<? extends FileHandle> fileClass) {
		this.fileClass = fileClass;
	}
	
	public Class<? extends FileHandle> getFileClass() {
		return fileClass;
	}
	
	
}
