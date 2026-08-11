package org.sagebionetworks.aws;

import java.nio.file.Path;
import java.util.List;

import org.sagebionetworks.aws.v2.S3ClientProvider;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.WebsiteConfiguration;

public class SynapseS3ClientV2Impl implements SynapseS3ClientV2 {

	private static final int NOT_FOUND_STATUS_CODE = 404;

	private final S3ClientProvider s3ClientProvider;

	public SynapseS3ClientV2Impl(S3ClientProvider s3ClientProvider) {
		this.s3ClientProvider = s3ClientProvider;
	}

	@Override
	public Region getRegionForBucketV2(String bucketName) throws CannotDetermineBucketLocationException {
		return s3ClientProvider.getRegionForBucket(bucketName);
	}

	@Override
	public HeadObjectResponse getObjectMetadataV2(String bucketName, String key) {
		return clientFor(bucketName).headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
	}

	@Override
	public DeleteObjectResponse deleteObjectV2(String bucketName, String key) {
		return clientFor(bucketName).deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
	}

	@Override
	public DeleteObjectsResponse deleteObjectsV2(DeleteObjectsRequest request) {
		return clientFor(request.bucket()).deleteObjects(request);
	}

	@Override
	public PutObjectResponse putObjectV2(PutObjectRequest request, RequestBody requestBody) {
		return clientFor(request.bucket()).putObject(request, requestBody);
	}

	@Override
	public ResponseInputStream<GetObjectResponse> getObjectV2(GetObjectRequest request) {
		return clientFor(request.bucket()).getObject(request);
	}

	@Override
	public GetObjectResponse getObjectV2(GetObjectRequest request, Path destinationFile) {
		return clientFor(request.bucket()).getObject(request, destinationFile);
	}

	@Override
	public ListObjectsV2Response listObjectsV2(ListObjectsV2Request request) {
		return clientFor(request.bucket()).listObjectsV2(request);
	}

	@Override
	public CreateBucketResponse createBucketV2(String bucketName) {
		return s3ClientProvider.getUsEast1Client()
				.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
	}

	@Override
	public boolean doesBucketExistV2(String bucketName) {
		try {
			s3ClientProvider.getUsEast1Client().headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
			return true;
		} catch (S3Exception e) {
			if (isNotFound(e)) {
				return false;
			}
			throw e;
		}
	}

	@Override
	public boolean doesObjectExistV2(String bucketName, String key) {
		try {
			getObjectMetadataV2(bucketName, key);
			return true;
		} catch (S3Exception e) {
			if (isNotFound(e)) {
				return false;
			}
			throw e;
		}
	}

	@Override
	public GetBucketCorsResponse getBucketCorsV2(String bucketName) {
		return clientFor(bucketName).getBucketCors(GetBucketCorsRequest.builder().bucket(bucketName).build());
	}

	@Override
	public void setBucketCorsV2(String bucketName, CORSConfiguration corsConfiguration) {
		clientFor(bucketName).putBucketCors(
				PutBucketCorsRequest.builder().bucket(bucketName).corsConfiguration(corsConfiguration).build());
	}

	@Override
	public void setBucketWebsiteConfigurationV2(String bucketName, WebsiteConfiguration configuration) {
		clientFor(bucketName).putBucketWebsite(
				PutBucketWebsiteRequest.builder().bucket(bucketName).websiteConfiguration(configuration).build());
	}

	@Override
	public void setBucketPolicyV2(String bucketName, String policyText) {
		clientFor(bucketName).putBucketPolicy(
				PutBucketPolicyRequest.builder().bucket(bucketName).policy(policyText).build());
	}

	@Override
	public GetObjectAclResponse getObjectAclV2(String bucketName, String key) {
		return clientFor(bucketName).getObjectAcl(GetObjectAclRequest.builder().bucket(bucketName).key(key).build());
	}

	@Override
	public String getAccountOwnerIdV2(String bucketName) {
		return clientFor(bucketName).listBuckets().owner().id();
	}

	@Override
	public CreateMultipartUploadResponse createMultipartUploadV2(CreateMultipartUploadRequest request) {
		return clientFor(request.bucket()).createMultipartUpload(request);
	}

	@Override
	public UploadPartResponse uploadPartV2(UploadPartRequest request, RequestBody requestBody) {
		return clientFor(request.bucket()).uploadPart(request, requestBody);
	}

	@Override
	public UploadPartCopyResponse uploadPartCopyV2(UploadPartCopyRequest request) {
		return clientFor(request.destinationBucket()).uploadPartCopy(request);
	}

	@Override
	public CompleteMultipartUploadResponse completeMultipartUploadV2(CompleteMultipartUploadRequest request) {
		return clientFor(request.bucket()).completeMultipartUpload(request);
	}

	@Override
	public AbortMultipartUploadResponse abortMultipartUploadV2(AbortMultipartUploadRequest request) {
		return clientFor(request.bucket()).abortMultipartUpload(request);
	}

	@Override
	public List<Tag> getObjectTagsV2(String bucketName, String key) {
		return clientFor(bucketName)
				.getObjectTagging(GetObjectTaggingRequest.builder().bucket(bucketName).key(key).build()).tagSet();
	}

	@Override
	public void setObjectTagsV2(String bucketName, String key, List<Tag> tags) {
		clientFor(bucketName).putObjectTagging(PutObjectTaggingRequest.builder().bucket(bucketName).key(key)
				.tagging(Tagging.builder().tagSet(tags).build()).build());
	}

	@Override
	public RestoreObjectResponse restoreObjectV2(RestoreObjectRequest request) {
		return clientFor(request.bucket()).restoreObject(request);
	}

	private S3Client clientFor(String bucketName) {
		return s3ClientProvider.getClientForBucket(bucketName);
	}

	// A missing bucket or key surfaces as a typed sub-class of S3Exception on some code paths and as a
	// plain 404 on others, so the status code is the reliable signal.
	private static boolean isNotFound(S3Exception e) {
		return e.statusCode() == NOT_FOUND_STATUS_CODE;
	}

}
