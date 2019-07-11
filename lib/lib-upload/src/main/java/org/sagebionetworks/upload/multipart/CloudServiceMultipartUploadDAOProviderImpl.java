package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.file.UploadType;

public class CloudServiceMultipartUploadDAOProviderImpl implements CloudServiceMultipartUploadDAOProvider {

	private static S3MultipartUploadDAOImpl s3MultipartUploadDAO = new S3MultipartUploadDAOImpl();
	private static GoogleCloudStorageMultipartUploadDAOImpl googleCloudStorageMultipartUploadDAO = new GoogleCloudStorageMultipartUploadDAOImpl();

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
