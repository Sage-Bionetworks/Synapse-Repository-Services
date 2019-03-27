package org.sagebionetworks.aws;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
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
import com.amazonaws.services.s3.model.S3Object;

public class SynapseS3ClientImpl implements SynapseS3Client {
	
	private AmazonS3 amazonS3;
	
	public SynapseS3ClientImpl(AmazonS3 amazonS3) {
		this.amazonS3=amazonS3;
	}

	@Override
	public ObjectMetadata getObjectMetadata(String bucketName, String key)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.getObjectMetadata(bucketName, key);
	}

	@Override
	public void deleteObject(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		amazonS3.deleteObject( bucketName,  key);
	}

	@Override
	public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.deleteObjects(deleteObjectsRequest);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.putObject( bucketName,  key,  input,  metadata);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, File file)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.putObject( bucketName, key,  file);
	}

	@Override
	public PutObjectResult putObject(PutObjectRequest putObjectRequest)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.putObject( putObjectRequest);
	}

	@Override
	public S3Object getObject(String bucketName, String key) throws SdkClientException, AmazonServiceException {
		return amazonS3.getObject( bucketName,  key);
	}

	@Override
	public S3Object getObject(GetObjectRequest getObjectRequest) throws SdkClientException, AmazonServiceException {
		return amazonS3.getObject( getObjectRequest);
	}

	@Override
	public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.getObject( getObjectRequest,  destinationFile);
	}

	@Override
	public ObjectListing listObjects(String bucketName, String prefix)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.listObjects( bucketName,  prefix);
	}

	@Override
	public ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.listObjects( listObjectsRequest);
	}

	@Override
	public Bucket createBucket(String bucketName) throws SdkClientException, AmazonServiceException {
		return amazonS3.createBucket( bucketName);
	}

	@Override
	public boolean doesObjectExist(String bucketName, String objectName)
			throws AmazonServiceException, SdkClientException {
		return amazonS3.doesObjectExist( bucketName,  objectName);
	}

	@Override
	public void setBucketCrossOriginConfiguration(String bucketName,
			BucketCrossOriginConfiguration bucketCrossOriginConfiguration) {
		amazonS3.setBucketCrossOriginConfiguration( bucketName,
				 bucketCrossOriginConfiguration);
	}

	@Override
	public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws SdkClientException {
		return amazonS3.generatePresignedUrl( generatePresignedUrlRequest);
	}

	@Override
	public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.initiateMultipartUpload( request);
	}

	@Override
	public CopyPartResult copyPart(CopyPartRequest copyPartRequest) throws SdkClientException, AmazonServiceException {
		return amazonS3.copyPart( copyPartRequest);
	}

	@Override
	public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request)
			throws SdkClientException, AmazonServiceException {
		return amazonS3.completeMultipartUpload( request);
	}

	@Override
	public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration)
			throws SdkClientException, AmazonServiceException {
		amazonS3.setBucketWebsiteConfiguration( bucketName,  configuration);
	}

	@Override
	public void setBucketPolicy(String bucketName, String policyText)
			throws SdkClientException, AmazonServiceException {
		amazonS3.setBucketPolicy( bucketName,  policyText);
	}

}
