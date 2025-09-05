package org.sagebionetworks.repo.manager.file;

import java.util.Map;

import org.sagebionetworks.repo.manager.file.readers.GCBucketObjectReader;
import org.sagebionetworks.repo.manager.file.readers.S3BucketObjectReader;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.stereotype.Service;

@Service
public class BucketObjectReaderProvider {
	
	private Map<Class<? extends CloudProviderFileHandleInterface>, BucketObjectReader> cloudFileReaderMap;
	
	public BucketObjectReaderProvider(S3BucketObjectReader s3Reader, GCBucketObjectReader gcsReader) {
		cloudFileReaderMap = Map.of(S3FileHandle.class, s3Reader, GoogleCloudFileHandle.class, gcsReader);
	}

	public BucketObjectReader getBucketObjectReader(Class<? extends CloudProviderFileHandleInterface> fileHandleClazz) {
		BucketObjectReader reader = cloudFileReaderMap.get(fileHandleClazz);
		if (reader == null) {
			throw new IllegalArgumentException("Unsupported file handle type " + fileHandleClazz.getName());
		}
		return reader;
	}
	
}
