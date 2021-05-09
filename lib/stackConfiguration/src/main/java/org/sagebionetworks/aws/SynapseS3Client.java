package org.sagebionetworks.aws;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

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
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;

/*
 * 
 * This is a facade for AmazonS3 (Amazon's S3 Client), exposing just the methods used by Synapse
 * and, in each method, doing the job of figuring out which region the given bucket is in, 
 * so that the S3 Client for that region is used.
 * 
 */
public interface SynapseS3Client {
	public ObjectMetadata getObjectMetadata(String bucketName, String key) throws SdkClientException, AmazonServiceException;

	public void deleteObject(String bucketName, String key) throws SdkClientException, AmazonServiceException;
	
	public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) throws SdkClientException, AmazonServiceException;

	public PutObjectResult putObject(
			String bucketName, String key, InputStream input, ObjectMetadata metadata)
					throws SdkClientException, AmazonServiceException;
	
	public PutObjectResult putObject(String bucketName, String key, File file)
			throws SdkClientException, AmazonServiceException;
	
	public PutObjectResult putObject(PutObjectRequest putObjectRequest)
			throws SdkClientException, AmazonServiceException;

	public S3Object getObject(String bucketName, String key) throws SdkClientException, AmazonServiceException;
	
	public S3Object getObject(GetObjectRequest getObjectRequest) throws SdkClientException, AmazonServiceException;
	
			ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile) throws SdkClientException, AmazonServiceException;

	public ObjectListing listObjects(String bucketName, String prefix)
			throws SdkClientException, AmazonServiceException;
	
	public ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
			throws SdkClientException, AmazonServiceException;

	public Bucket createBucket(String bucketName)
			throws SdkClientException, AmazonServiceException;

	boolean doesObjectExist(String bucketName, String objectName)
			throws AmazonServiceException, SdkClientException;

	public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName);

	public void setBucketCrossOriginConfiguration(String bucketName, BucketCrossOriginConfiguration bucketCrossOriginConfiguration);

	public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest)
			throws SdkClientException;

	public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request)
			throws SdkClientException, AmazonServiceException;

	public CopyPartResult copyPart(CopyPartRequest copyPartRequest) throws SdkClientException, AmazonServiceException;

	public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
			throws SdkClientException, AmazonServiceException;
	
	void abortMultipartUpload(AbortMultipartUploadRequest request) throws SdkClientException, AmazonServiceException;

	public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration)
			throws SdkClientException, AmazonServiceException;

	public void setBucketPolicy(String bucketName, String policyText)
			throws SdkClientException, AmazonServiceException;
	
	public AccessControlList getObjectAcl(String bucketName, String objectName);
	
	public String getAccountOwnerId(String bucketName);

	/*
	 * Return the Amazon S3 client for the US Standard Region
	 */
	public AmazonS3 getUSStandardAmazonClient();
	
	/**
	 * Find the Region for the given bucket.  
	 * 
	 * @param bucketName
	 * @return the Region for the bucket having the given name
	 * @throws CannotDetermineBucketLocationException if the bucket doesn't grant 's3:GetBucketLocation' permission
	 */
	public Region getRegionForBucket(String bucketName) throws CannotDetermineBucketLocationException;


}
