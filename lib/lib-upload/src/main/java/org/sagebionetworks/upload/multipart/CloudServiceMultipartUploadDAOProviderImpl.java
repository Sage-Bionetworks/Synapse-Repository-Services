package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.file.UploadType;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudServiceMultipartUploadDAOProviderImpl implements CloudServiceMultipartUploadDAOProvider {

	@Autowired
	private S3MultipartUploadDAOImpl s3MultipartUploadDAO;

	@Autowired
	private GoogleCloudStorageMultipartUploadDAOImpl googleCloudStorageMultipartUploadDAO;

	@Autowired
	private AsyncGoogleMultipartUploadDao asyncGoogleMultipartUploadDAO;

	@Autowired
	private FeatureStatusDao featureStatusDao;

	@Override
	public CloudServiceMultipartUploadDAO getCloudServiceMultipartUploadDao(UploadType uploadType) {
		switch (uploadType) {
		case S3:
			return s3MultipartUploadDAO;
		case GOOGLECLOUDSTORAGE:
			if (featureStatusDao.isFeatureEnabled(Feature.USE_NEW_ASYNC_GOOGLE_MULTIPART_UPLOAD).orElse(true)) {
				return asyncGoogleMultipartUploadDAO;
			} else {
				return googleCloudStorageMultipartUploadDAO;
			}
		default:
			throw new IllegalArgumentException(
					"Multipart upload for upload type " + uploadType.toString() + " is not supported.");
		}
	}
}
