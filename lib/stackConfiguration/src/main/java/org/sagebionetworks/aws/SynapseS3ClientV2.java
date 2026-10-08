package org.sagebionetworks.aws;

import java.nio.file.Path;
import java.util.List;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectResponse;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.WebsiteConfiguration;

/**
 * The AWS SDK v2 half of the Synapse S3 facade: the same "route the call to the client of the
 * bucket's region" service, expressed with v2 model types.
 * <p>
 * Every method carries a {@code V2} suffix so that it can sit beside its SDK v1 sibling on
 * {@link SynapseS3Client} while callers migrate. The suffix is dropped once SDK v1 is gone
 * (PLFM-9749).
 * <p>
 * The deprecated ListObjects API has no sibling here: callers of
 * {@code SynapseS3Client.listObjects} move to {@link #listObjectsV2(ListObjectsV2Request)} when
 * they migrate.
 */
public interface SynapseS3ClientV2 {

	/**
	 * Find the region of the given bucket.
	 *
	 * @param bucketName The name of the bucket
	 * @return The region the bucket resides in
	 * @throws CannotDetermineBucketLocationException If the region of the bucket cannot be determined
	 */
	Region getRegionForBucketV2(String bucketName) throws CannotDetermineBucketLocationException;

	/**
	 * @param bucketName The name of the bucket holding the object
	 * @param key        The object key
	 * @return The metadata of the object
	 */
	HeadObjectResponse getObjectMetadataV2(String bucketName, String key);

	/**
	 * @param bucketName The name of the bucket holding the object
	 * @param key        The key of the object to delete
	 */
	DeleteObjectResponse deleteObjectV2(String bucketName, String key);

	DeleteObjectsResponse deleteObjectsV2(DeleteObjectsRequest request);

	/**
	 * Store an object.
	 *
	 * @param request     Describes the target bucket, key and object metadata
	 * @param requestBody The content of the object. Note that unlike SDK v1, streaming content
	 *                    requires the length to be known upfront, see
	 *                    {@link RequestBody#fromInputStream(java.io.InputStream, long)}
	 */
	PutObjectResponse putObjectV2(PutObjectRequest request, RequestBody requestBody);

	/**
	 * Open a stream over the content of an object. The caller owns the returned stream and must
	 * close it, note that closing before the content is fully read aborts the underlying connection.
	 *
	 * @param request Describes the object to read
	 * @return The content of the object, with the object metadata attached to the response
	 */
	ResponseInputStream<GetObjectResponse> getObjectV2(GetObjectRequest request);

	/**
	 * Download an object to a local file.
	 *
	 * @param request         Describes the object to read
	 * @param destinationFile The file to write the content to, replaced if it already exists
	 * @return The metadata of the downloaded object
	 */
	GetObjectResponse getObjectV2(GetObjectRequest request, Path destinationFile);

	/**
	 * List a page of the objects in a bucket using the S3 ListObjectsV2 API.
	 */
	ListObjectsV2Response listObjectsV2(ListObjectsV2Request request);

	CreateBucketResponse createBucketV2(String bucketName);

	/**
	 * @param bucketName The name of the bucket
	 * @return True if the bucket exists and is visible to Synapse
	 */
	boolean doesBucketExistV2(String bucketName);

	/**
	 * @param bucketName The name of the bucket holding the object
	 * @param key        The object key
	 * @return True if an object with the given key exists in the bucket
	 */
	boolean doesObjectExistV2(String bucketName, String key);

	GetBucketCorsResponse getBucketCorsV2(String bucketName);

	void setBucketCorsV2(String bucketName, CORSConfiguration corsConfiguration);

	void setBucketWebsiteConfigurationV2(String bucketName, WebsiteConfiguration configuration);

	void setBucketPolicyV2(String bucketName, String policyText);

	GetObjectAclResponse getObjectAclV2(String bucketName, String key);

	/**
	 * @param bucketName The name of the bucket, used to select the client region
	 * @return The id of the account owning the S3 resources reachable by Synapse in that region
	 */
	String getAccountOwnerIdV2(String bucketName);

	CreateMultipartUploadResponse createMultipartUploadV2(CreateMultipartUploadRequest request);

	/**
	 * Upload a single part of a multi-part upload.
	 *
	 * @param request     Describes the target upload and part number
	 * @param requestBody The content of the part
	 */
	UploadPartResponse uploadPartV2(UploadPartRequest request, RequestBody requestBody);

	/**
	 * Copy an existing object, or a range of it, into a single part of a multi-part upload. This is
	 * the SDK v2 replacement for the v1 {@code copyPart} operation.
	 */
	UploadPartCopyResponse uploadPartCopyV2(UploadPartCopyRequest request);

	CompleteMultipartUploadResponse completeMultipartUploadV2(CompleteMultipartUploadRequest request);

	AbortMultipartUploadResponse abortMultipartUploadV2(AbortMultipartUploadRequest request);

	/**
	 * @param bucketName The bucket name
	 * @param key        The object key
	 * @return The set of tags for the object in the given bucket
	 */
	List<Tag> getObjectTagsV2(String bucketName, String key);

	/**
	 * Updates the object tags for the given object
	 *
	 * @param bucketName The bucket name
	 * @param key        The object key
	 * @param tags       The set of tags for the object (will replace the existing ones)
	 */
	void setObjectTagsV2(String bucketName, String key, List<Tag> tags);

	RestoreObjectResponse restoreObjectV2(RestoreObjectRequest request);

}
