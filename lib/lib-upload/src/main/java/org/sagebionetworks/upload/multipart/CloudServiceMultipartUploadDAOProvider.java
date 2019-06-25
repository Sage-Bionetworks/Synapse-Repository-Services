package org.sagebionetworks.upload.multipart;

import org.sagebionetworks.repo.model.file.UploadType;

/**
 * The operations for multipart upload are dependent upon the actual service. This interface defines the provider
 * that can supply the correct upload DAO for a particular cloud service.
 */
public interface CloudServiceMultipartUploadDAOProvider {

	/**
	 * Get the correct cloud service multipart upload DAO for the chosen upload type.
	 * @param uploadType
	 * @return
	 */
	CloudServiceMultipartUploadDAO getCloudServiceMultipartUploadDao(UploadType uploadType);
}
