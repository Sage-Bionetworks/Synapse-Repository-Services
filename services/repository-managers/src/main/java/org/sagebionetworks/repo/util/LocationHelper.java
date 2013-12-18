package org.sagebionetworks.repo.util;

import org.sagebionetworks.repo.model.DatastoreException;
import org.springframework.http.HttpMethod;

import com.amazonaws.services.securitytoken.model.Credentials;

/**
 * @author deflaux
 * 
 */
public interface LocationHelper {

	/**
	 * Return a pre-signed URL for use in downloading files from S3. The
	 * returned URL will be valid for a GET request of a single object in S3.
	 * Note that authentication and authorization should have been checked
	 * *prior* to calling this method.
	 * 
	 * Note that the AWS libraries throw many runtime exceptions (e.g., if stuff
	 * times out). The user should retry what ever it is they were trying to do
	 * if they get one of those errors. Our code here should not perform the
	 * retry, but should instead handle situations where prior operations were
	 * only partially completed such as we created a new IAM user but we were
	 * not able to add them to the right IAM group.
	 * 
	 * @param userId
	 * @param path
	 *            the s3key for the item
	 * @return a pre-signed S3 URL valid for GET requests
	 * @throws DatastoreException
	 */
	String presignS3GETUrl(Long userId, String path) throws DatastoreException;
	
	/**
	 * Return a pre-signed URL for use in downloading files from S3. The
	 * returned URL will be valid for a GET request of a single object in S3.
	 * Note that authentication and authorization should have been checked
	 * *prior* to calling this method.
	 * 
	 * Note that the AWS libraries throw many runtime exceptions (e.g., if stuff
	 * times out). The user should retry what ever it is they were trying to do
	 * if they get one of those errors. Our code here should not perform the
	 * retry, but should instead handle situations where prior operations were
	 * only partially completed such as we created a new IAM user but we were
	 * not able to add them to the right IAM group.
	 * 
	 * @param userId
	 * @param s3Key 
	 * @param expiresSeconds 
	 * @return a pre-signed S3 URL valid for GET requests
	 * @throws DatastoreException
	 */
	String presignS3GETUrl(Long userId, String s3Key, int expiresSeconds)
			throws DatastoreException;

	/**
	 * Return a pre-signed URL for use in downloading files from S3. The
	 * returned URL will be valid for a GET request of a single object in S3.
	 * Note that authentication and authorization should have been checked
	 * *prior* to calling this method.
	 * 
	 * This version will return a URL that is only good for 10 seconds.
	 * 
	 * @param userId
	 * @param path
	 *            the s3key for the item
	 * @return a pre-signed S3 URL valid for GET requests
	 * @throws DatastoreException
	 */
	String presignS3GETUrlShortLived(Long userId, String path) throws DatastoreException;

	/**
	 * Return a pre-signed URL for use checking the status of files in S3, such
	 * as the current MD5 checksum. The returned URL will be valid for a HEAD
	 * request of a single object in S3. Note that authentication and
	 * authorization should have been checked *prior* to calling this method.
	 * 
	 * Note that the AWS libraries throw many runtime exceptions (e.g., if stuff
	 * times out). The user should retry what ever it is they were trying to do
	 * if they get one of those errors. Our code here should not perform the
	 * retry, but should instead handle situations where prior operations were
	 * only partially completed such as we created a new IAM user but we were
	 * not able to add them to the right IAM group.
	 * 
	 * @param userId
	 * @param path
	 *            the s3key for the item
	 * @return a pre-signed S3 URL valid for HEAD requests
	 * @throws DatastoreException
	 */
	String presignS3HEADUrl(Long userId, String path) throws DatastoreException;

	/**
	 * Return a pre-signed URL for use checking the status of files in S3, such
	 * as the current MD5 checksum. The returned URL will be valid for a HEAD
	 * request of a single object in S3. Note that authentication and
	 * authorization should have been checked *prior* to calling this method.
	 * 
	 * Note that the AWS libraries throw many runtime exceptions (e.g., if stuff
	 * times out). The user should retry what ever it is they were trying to do
	 * if they get one of those errors. Our code here should not perform the
	 * retry, but should instead handle situations where prior operations were
	 * only partially completed such as we created a new IAM user but we were
	 * not able to add them to the right IAM group.
	 * 
	 * @param userId
	 * @param s3Key 
	 * @param expiresSeconds 
	 * @return a pre-signed S3 URL valid for HEAD requests
	 * @throws DatastoreException
	 */
	String presignS3HEADUrl(Long userId, String s3Key, int expiresSeconds)
			throws DatastoreException;

	/**
	 * Return a pre-signed URL for use in uploading a file to S3. Note that the
	 * MD5 passed here must match the MD5 of the file uploaded or S3 will reject
	 * the request. The returned URL will be valid for a PUT request of a single
	 * object to S3. Note that authentication and authorization should have been
	 * checked *prior* to calling this method.
	 * 
	 * Note that the AWS libraries throw many runtime exceptions (e.g., if stuff
	 * times out). The user should retry what ever it is they were trying to do
	 * if they get one of those errors. Our code here should not perform the
	 * retry, but should instead handle situations where prior operations were
	 * only partially completed such as we created a new IAM user but we were
	 * not able to add them to the right IAM group.
	 * 
	 * @param userId
	 * @param path
	 * @param md5
	 * @param contentType
	 * @return a pre-signed URL valid for PUT requests
	 * @throws DatastoreException
	 */
	String presignS3PUTUrl(Long userId, String path, String md5, String contentType)
			throws DatastoreException;
	
	/**
	 * 
	 * @param sessionCredentials
	 * @param s3Key
	 * @param md5
	 * @param contentType
	 * @return a pre-signed URL valid for PUT requests
	 * @throws DatastoreException
	 */
	String presignS3PUTUrl(Credentials sessionCredentials, String s3Key,
			String md5, String contentType) throws DatastoreException;
	
	/**
	 * Create a federation token for an S3 object
	 * 
	 * @param userId
	 * @param method
	 * @param s3Key
	 * @return the securityToken credentials
	 * @throws DatastoreException 
	 * @throws NumberFormatException 
	 */
	Credentials createFederationTokenForS3(Long userId, HttpMethod method,
			String s3Key) throws NumberFormatException, DatastoreException;

	/**
	 * Retrieve just the s3Key portion of an S3 URL
	 * 
	 * @param s3Url
	 * @return the s3Key
	 */
	String getS3KeyFromS3Url(String s3Url);

	/**
	 * Retrieve just the entity id from an S3 URL or S3 key
	 * 
	 * @param s3Url
	 * @return the entity id
	 * @throws DatastoreException 
	 * @throws NumberFormatException 
	 */
	String getEntityIdFromS3Url(String s3Url) throws NumberFormatException, DatastoreException;

}
