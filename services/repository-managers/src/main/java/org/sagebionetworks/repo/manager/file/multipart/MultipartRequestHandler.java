package org.sagebionetworks.repo.manager.file.multipart;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.upload.multipart.PresignedUrl;

/**
 * Abstraction to handle different types of multipart requests.
 * 
 * @author Marco Marasca
 *
 */
public interface MultipartRequestHandler<T extends MultipartRequest> {
	
	/**
	 * @return The concrete request class
	 */
	Class<T> getRequestClass();

	/**
	 * Validates the given request, generic validation on the {@link MultipartRequest} is performed before invoking this method
	 * 
	 * @param user The user making the request
	 * @param request The multipart request to validate
	 */
	void validateRequest(UserInfo user, T request);
	
	/**
	 * Initiates a the multipart request
	 * 
	 * @param user The user initiating the request
	 * @param request The original request
	 * @param requestHash The hash of the request
	 * @param storageLocation The destination storage location, might be null
	 * @return A {@link CreateMultipartRequest} DTO that encapsulate the data about the new request
	 */
	CreateMultipartRequest initiateRequest(UserInfo user, T request, String requestHash, StorageLocationSetting storageLocation);
	
	/**
	 * Obtains a pre-signed url for the given upload status and part number
	 * 
	 * @param status The upload status
	 * @param partNumber The part number
	 * @param contentType The optional content type
	 * @return A pre-signed url including all the signed headers
	 */
	PresignedUrl getPresignedUrl(CompositeMultipartUploadStatus status, long partNumber, String contentType);
	
	/**
	 * Invoked after a part was added to the multipart
	 * 
	 * @param status The upload status
	 * @param partNumber The part number
	 * @param partMD5Hex The part MD5 checksum
	 */
	void validateAddedPart(CompositeMultipartUploadStatus status, long partNumber, String partMD5Hex);
	
	/**
	 * Invoked after a multipart is completed in order to obtain the data for a new file handle
	 * 
	 * @param status The upload status
	 * @param originalRequest The original request, serialized to json
	 * @return The data about the new file handle to create
	 */
	FileHandleCreateRequest getFileHandleCreateRequest(CompositeMultipartUploadStatus status, String originalRequest);
	
	/**
	 * Aborts the multi part request clearing any temporary data that was stored during the upload
	 * 
	 * @param status The multipart status
	 */
	void tryAbortMultipartRequest(CompositeMultipartUploadStatus status);
	
}
