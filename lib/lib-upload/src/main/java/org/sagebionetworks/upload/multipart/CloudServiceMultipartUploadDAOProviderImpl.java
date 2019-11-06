package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.file.UploadType;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudServiceMultipartUploadDAOProviderImpl implements CloudServiceMultipartUploadDAOProvider {

	@Autowired
	private S3MultipartUploadDAOImpl s3MultipartUploadDAO;

	@Autowired
	private GoogleCloudStorageMultipartUploadDAOImpl googleCloudStorageMultipartUploadDAO;

	@Override
	public CloudServiceMultipartUploadDAO getCloudServiceMultipartUploadDao(UploadType uploadType) {
		switch (uploadType) {
			case S3:
				return s3MultipartUploadDAO;
			case GOOGLECLOUDSTORAGE:
				return googleCloudStorageMultipartUploadDAO;
			default:
				throw new IllegalArgumentException("Multipart upload for upload type " + uploadType.toString() + " is not supported.");
		}
	}
}
