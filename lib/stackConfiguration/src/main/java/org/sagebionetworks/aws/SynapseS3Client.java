package org.sagebionetworks.aws;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

/*
 * 
 * This is a facade for AmazonS3 (Amazon's S3 Client), exposing just the methods used by Synapse
 * and, in each method, doing the job of figuring out which region the given bucket is in, 
 * so that the S3 Client for that region is used.
 * 
 */
public interface SynapseS3Client {
	
	AmazonS3 getS3ClientForBucket(String bucket);
	

	default ObjectMetadata getObjectMetadata(String bucketName, String key)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(bucketName).getObjectMetadata(bucketName, key);
	}

	default void deleteObject(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		getS3ClientForBucket(bucketName).deleteObject( bucketName,  key);
	}

	default DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(deleteObjectsRequest.getBucketName()).deleteObjects(deleteObjectsRequest);
	}

	default PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(bucketName).putObject(bucketName,  key,  input,  metadata);
	}

	default PutObjectResult putObject(String bucketName, String key, File file)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(bucketName).putObject( bucketName, key,  file);
	}

	default PutObjectResult putObject(PutObjectRequest putObjectRequest)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(putObjectRequest.getBucketName()).putObject( putObjectRequest);
	}

	default S3Object getObject(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(bucketName).getObject( bucketName,  key);
	}

	default S3Object getObject(GetObjectRequest getObjectRequest) throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(getObjectRequest.getBucketName()).getObject(getObjectRequest);
	}

	default ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(getObjectRequest.getBucketName()).getObject( getObjectRequest,  destinationFile);
	}
	
	default AccessControlList getObjectAcl(String bucketName, String objectName) {
		return getS3ClientForBucket(bucketName).getObjectAcl(bucketName, objectName);
	}

	default ObjectListing listObjects(String bucketName, String prefix)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(bucketName).listObjects( bucketName,  prefix);
	}

	default ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(listObjectsRequest.getBucketName()).listObjects( listObjectsRequest);
	}

	default ListObjectsV2Result listObjectsV2(ListObjectsV2Request listObjectsV2Request)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(listObjectsV2Request.getBucketName()).listObjectsV2(listObjectsV2Request);
	}

	Bucket createBucket(String bucketName) throws SdkClientException, AmazonServiceException;


	default boolean doesObjectExist(String bucketName, String objectName)
			throws AmazonServiceException, SdkClientException {
		return getS3ClientForBucket(bucketName).doesObjectExist( bucketName,  objectName);
	}


	default void setBucketCrossOriginConfiguration(String bucketName,
			BucketCrossOriginConfiguration bucketCrossOriginConfiguration) {
		getS3ClientForBucket(bucketName).setBucketCrossOriginConfiguration( bucketName,
				bucketCrossOriginConfiguration);
	}

	default URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws SdkClientException {
		return getS3ClientForBucket(generatePresignedUrlRequest.getBucketName()).generatePresignedUrl( generatePresignedUrlRequest);
	}


	default InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(request.getBucketName()).initiateMultipartUpload(request);
	}


	default CopyPartResult copyPart(CopyPartRequest copyPartRequest) throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(copyPartRequest.getDestinationBucketName()).copyPart(copyPartRequest);
	}


	default CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
			throws SdkClientException, AmazonServiceException {
		return getS3ClientForBucket(request.getBucketName()).completeMultipartUpload(request);
	}

	default void abortMultipartUpload(AbortMultipartUploadRequest request) throws SdkClientException, AmazonServiceException {
		getS3ClientForBucket(request.getBucketName()).abortMultipartUpload(request);
	}


	default void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration)
			throws SdkClientException, AmazonServiceException {
		getS3ClientForBucket(bucketName).setBucketWebsiteConfiguration(bucketName,  configuration);
	}


	default void setBucketPolicy(String bucketName, String policyText)
			throws SdkClientException, AmazonServiceException {
		getS3ClientForBucket(bucketName).setBucketPolicy(bucketName,  policyText);
	}


	default BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName) {
		return getS3ClientForBucket(bucketName).getBucketCrossOriginConfiguration(bucketName);
	}
	
	default String getAccountOwnerId(String bucketName) {
		return getS3ClientForBucket(bucketName).getS3AccountOwner().getId();
	}
	
	default List<Tag> getObjectTags(String bucketName, String key) {
		GetObjectTaggingRequest request = new GetObjectTaggingRequest(bucketName, key);
		
		GetObjectTaggingResult response = getS3ClientForBucket(bucketName).getObjectTagging(request);
		
		return response.getTagSet();
	}
	
	default void setObjectTags(String bucketName, String key, List<Tag> tags) {
		SetObjectTaggingRequest request = new SetObjectTaggingRequest(bucketName, key, new ObjectTagging(tags));
		
		getS3ClientForBucket(bucketName).setObjectTagging(request);
	}
	
	default RestoreObjectResult restoreObject(RestoreObjectRequest request) {
		return getS3ClientForBucket(request.getBucketName()).restoreObjectV2(request);
	}
	
	default UploadPartResult uploadPart(UploadPartRequest uploadPartRequest) {
		return getS3ClientForBucket(uploadPartRequest.getBucketName()).uploadPart(uploadPartRequest);
	}


	Region getRegionForBucket(String sourceBucket);


	AmazonS3 getUSStandardAmazonClient();


}
