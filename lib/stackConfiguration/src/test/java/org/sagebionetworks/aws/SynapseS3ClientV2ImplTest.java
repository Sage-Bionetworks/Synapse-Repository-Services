package org.sagebionetworks.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sagebionetworks.aws.v2.S3ClientProvider;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
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
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.Owner;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;
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

@ExtendWith(MockitoExtension.class)
public class SynapseS3ClientV2ImplTest {

	@Mock
	private S3ClientProvider mockS3ClientProvider;

	@Mock
	private S3Client mockBucketClient;

	@Mock
	private S3Client mockUsEast1Client;

	@Mock
	private ResponseInputStream<GetObjectResponse> mockObjectStream;

	@InjectMocks
	private SynapseS3ClientV2Impl client;

	private static final String BUCKET_NAME = "bucket-name";

	private static final String OBJECT_KEY = "s3-object-key";

	private static final String UPLOAD_ID = "upload-id";

	// Stubbing the lookup by bucket name means each test also proves the call was routed through the
	// client of the bucket's region rather than a default client.
	private void setupBucketClient() {
		when(mockS3ClientProvider.getClientForBucket(BUCKET_NAME)).thenReturn(mockBucketClient);
	}

	private static S3Exception s3Exception(int statusCode, String message) {
		return (S3Exception) S3Exception.builder().statusCode(statusCode).message(message).build();
	}

	@Test
	public void testGetRegionForBucketV2() {
		when(mockS3ClientProvider.getRegionForBucket(BUCKET_NAME)).thenReturn(Region.US_WEST_2);

		// call under test
		assertEquals(Region.US_WEST_2, client.getRegionForBucketV2(BUCKET_NAME));
	}

	@Test
	public void testGetRegionForBucketV2WithUnknownRegion() {
		CannotDetermineBucketLocationException expected = new CannotDetermineBucketLocationException("no region");

		when(mockS3ClientProvider.getRegionForBucket(BUCKET_NAME)).thenThrow(expected);

		// call under test
		CannotDetermineBucketLocationException result = assertThrows(CannotDetermineBucketLocationException.class,
				() -> client.getRegionForBucketV2(BUCKET_NAME));

		assertSame(expected, result);
	}

	@Test
	public void testGetObjectMetadataV2() {
		setupBucketClient();

		HeadObjectResponse expected = HeadObjectResponse.builder().contentLength(123L).build();

		when(mockBucketClient.headObject(any(HeadObjectRequest.class))).thenReturn(expected);

		// call under test
		HeadObjectResponse result = client.getObjectMetadataV2(BUCKET_NAME, OBJECT_KEY);

		assertEquals(expected, result);
		verify(mockBucketClient)
				.headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build());
	}

	@Test
	public void testDeleteObjectV2() {
		setupBucketClient();

		DeleteObjectResponse expected = DeleteObjectResponse.builder().build();

		when(mockBucketClient.deleteObject(any(DeleteObjectRequest.class))).thenReturn(expected);

		// call under test
		DeleteObjectResponse result = client.deleteObjectV2(BUCKET_NAME, OBJECT_KEY);

		assertEquals(expected, result);
		verify(mockBucketClient)
				.deleteObject(DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build());
	}

	@Test
	public void testDeleteObjectsV2() {
		setupBucketClient();

		DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket(BUCKET_NAME).build();
		DeleteObjectsResponse expected = DeleteObjectsResponse.builder().build();

		when(mockBucketClient.deleteObjects(request)).thenReturn(expected);

		// call under test
		DeleteObjectsResponse result = client.deleteObjectsV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).deleteObjects(request);
	}

	@Test
	public void testPutObjectV2() {
		setupBucketClient();

		PutObjectRequest request = PutObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build();
		RequestBody body = RequestBody.fromString("content");
		PutObjectResponse expected = PutObjectResponse.builder().eTag("etag").build();

		when(mockBucketClient.putObject(request, body)).thenReturn(expected);

		// call under test
		PutObjectResponse result = client.putObjectV2(request, body);

		assertEquals(expected, result);
		verify(mockBucketClient).putObject(request, body);
	}

	@Test
	public void testGetObjectV2() {
		setupBucketClient();

		GetObjectRequest request = GetObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build();

		when(mockBucketClient.getObject(request)).thenReturn(mockObjectStream);

		// call under test
		ResponseInputStream<GetObjectResponse> result = client.getObjectV2(request);

		assertSame(mockObjectStream, result);
		verify(mockBucketClient).getObject(request);
	}

	@Test
	public void testGetObjectV2WithDestinationFile() throws Exception {
		setupBucketClient();

		GetObjectRequest request = GetObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build();
		GetObjectResponse expected = GetObjectResponse.builder().contentLength(11L).build();
		Path destination = Files.createTempFile("SynapseS3ClientV2ImplTest", ".tmp");

		try {
			stubDownloadOf(expected, "new content");

			// call under test
			GetObjectResponse result = client.getObjectV2(request, destination);

			assertEquals(expected, result);
			assertEquals("new content", readContent(destination));
		} finally {
			Files.deleteIfExists(destination);
		}
	}

	@Test
	public void testGetObjectV2WithExistingDestinationFile() throws Exception {
		setupBucketClient();

		GetObjectRequest request = GetObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build();
		GetObjectResponse expected = GetObjectResponse.builder().contentLength(11L).build();
		Path destination = Files.createTempFile("SynapseS3ClientV2ImplTest", ".tmp");

		try {
			Files.write(destination, "stale content".getBytes(StandardCharsets.UTF_8));
			stubDownloadOf(expected, "new content");

			// call under test
			GetObjectResponse result = client.getObjectV2(request, destination);

			assertEquals(expected, result);
			assertEquals("new content", readContent(destination));
		} finally {
			Files.deleteIfExists(destination);
		}
	}

	private static String readContent(Path file) throws Exception {
		return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
	}

	/**
	 * Hand the given content to whatever {@link ResponseTransformer} the facade builds, the way the
	 * real client would once the response body starts streaming.
	 */
	private void stubDownloadOf(GetObjectResponse response, String content) {
		when(mockBucketClient.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
				.thenAnswer(invocation -> {
					ResponseTransformer<GetObjectResponse, GetObjectResponse> transformer = invocation.getArgument(1);
					return transformer.transform(response, AbortableInputStream
							.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
				});
	}

	@Test
	public void testListObjectsV2() {
		setupBucketClient();

		ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(BUCKET_NAME).prefix("PRE").build();
		ListObjectsV2Response expected = ListObjectsV2Response.builder().keyCount(0).build();

		when(mockBucketClient.listObjectsV2(request)).thenReturn(expected);

		// call under test
		ListObjectsV2Response result = client.listObjectsV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).listObjectsV2(request);
	}

	@Test
	public void testCreateBucketV2() {
		when(mockS3ClientProvider.getUsEast1Client()).thenReturn(mockUsEast1Client);

		CreateBucketResponse expected = CreateBucketResponse.builder().build();

		when(mockUsEast1Client.createBucket(any(CreateBucketRequest.class))).thenReturn(expected);

		// call under test
		CreateBucketResponse result = client.createBucketV2(BUCKET_NAME);

		assertEquals(expected, result);
		verify(mockUsEast1Client).createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
		verify(mockS3ClientProvider, never()).getClientForBucket(any());
	}

	@Test
	public void testDoesBucketExistV2() {
		when(mockS3ClientProvider.getUsEast1Client()).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

		// call under test
		assertTrue(client.doesBucketExistV2(BUCKET_NAME));

		verify(mockUsEast1Client).headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
	}

	@Test
	public void testDoesBucketExistV2WithMissingBucket() {
		when(mockS3ClientProvider.getUsEast1Client()).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Exception(404, "Not found"));

		// call under test
		assertFalse(client.doesBucketExistV2(BUCKET_NAME));
	}

	@Test
	public void testDoesBucketExistV2WithOtherError() {
		when(mockS3ClientProvider.getUsEast1Client()).thenReturn(mockUsEast1Client);

		S3Exception expected = s3Exception(403, "Forbidden");

		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class))).thenThrow(expected);

		// call under test
		S3Exception result = assertThrows(S3Exception.class, () -> client.doesBucketExistV2(BUCKET_NAME));

		assertSame(expected, result);
	}

	@Test
	public void testDoesObjectExistV2() {
		setupBucketClient();

		when(mockBucketClient.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

		// call under test
		assertTrue(client.doesObjectExistV2(BUCKET_NAME, OBJECT_KEY));

		verify(mockBucketClient).headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build());
	}

	@Test
	public void testDoesObjectExistV2WithMissingKey() {
		setupBucketClient();

		when(mockBucketClient.headObject(any(HeadObjectRequest.class)))
				.thenThrow(NoSuchKeyException.builder().statusCode(404).message("No such key").build());

		// call under test
		assertFalse(client.doesObjectExistV2(BUCKET_NAME, OBJECT_KEY));
	}

	@Test
	public void testDoesObjectExistV2WithOtherError() {
		setupBucketClient();

		S3Exception expected = s3Exception(403, "Forbidden");

		when(mockBucketClient.headObject(any(HeadObjectRequest.class))).thenThrow(expected);

		// call under test
		S3Exception result = assertThrows(S3Exception.class, () -> client.doesObjectExistV2(BUCKET_NAME, OBJECT_KEY));

		assertSame(expected, result);
	}

	@Test
	public void testGetBucketCorsV2() {
		setupBucketClient();

		GetBucketCorsResponse expected = GetBucketCorsResponse.builder()
				.corsRules(CORSRule.builder().allowedMethods("GET").build()).build();

		when(mockBucketClient.getBucketCors(any(GetBucketCorsRequest.class))).thenReturn(expected);

		// call under test
		GetBucketCorsResponse result = client.getBucketCorsV2(BUCKET_NAME);

		assertEquals(expected, result);
		verify(mockBucketClient).getBucketCors(GetBucketCorsRequest.builder().bucket(BUCKET_NAME).build());
	}

	@Test
	public void testSetBucketCorsV2() {
		setupBucketClient();

		CORSConfiguration configuration = CORSConfiguration.builder()
				.corsRules(CORSRule.builder().allowedMethods("GET").build()).build();

		// call under test
		client.setBucketCorsV2(BUCKET_NAME, configuration);

		verify(mockBucketClient).putBucketCors(
				PutBucketCorsRequest.builder().bucket(BUCKET_NAME).corsConfiguration(configuration).build());
	}

	@Test
	public void testSetBucketWebsiteConfigurationV2() {
		setupBucketClient();

		WebsiteConfiguration configuration = WebsiteConfiguration.builder().build();

		// call under test
		client.setBucketWebsiteConfigurationV2(BUCKET_NAME, configuration);

		verify(mockBucketClient).putBucketWebsite(
				PutBucketWebsiteRequest.builder().bucket(BUCKET_NAME).websiteConfiguration(configuration).build());
	}

	@Test
	public void testSetBucketPolicyV2() {
		setupBucketClient();

		String policyText = "policy";

		// call under test
		client.setBucketPolicyV2(BUCKET_NAME, policyText);

		verify(mockBucketClient)
				.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(BUCKET_NAME).policy(policyText).build());
	}

	@Test
	public void testGetObjectAclV2() {
		setupBucketClient();

		GetObjectAclResponse expected = GetObjectAclResponse.builder()
				.owner(Owner.builder().id("owner-id").build()).build();

		when(mockBucketClient.getObjectAcl(any(GetObjectAclRequest.class))).thenReturn(expected);

		// call under test
		GetObjectAclResponse result = client.getObjectAclV2(BUCKET_NAME, OBJECT_KEY);

		assertEquals(expected, result);
		verify(mockBucketClient)
				.getObjectAcl(GetObjectAclRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build());
	}

	@Test
	public void testGetAccountOwnerIdV2() {
		setupBucketClient();

		when(mockBucketClient.listBuckets())
				.thenReturn(ListBucketsResponse.builder().owner(Owner.builder().id("owner-id").build()).build());

		// call under test
		String result = client.getAccountOwnerIdV2(BUCKET_NAME);

		assertEquals("owner-id", result);
	}

	@Test
	public void testCreateMultipartUploadV2() {
		setupBucketClient();

		CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder().bucket(BUCKET_NAME)
				.key(OBJECT_KEY).build();
		CreateMultipartUploadResponse expected = CreateMultipartUploadResponse.builder().uploadId(UPLOAD_ID).build();

		when(mockBucketClient.createMultipartUpload(request)).thenReturn(expected);

		// call under test
		CreateMultipartUploadResponse result = client.createMultipartUploadV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).createMultipartUpload(request);
	}

	@Test
	public void testUploadPartV2() {
		setupBucketClient();

		UploadPartRequest request = UploadPartRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY)
				.uploadId(UPLOAD_ID).partNumber(1).build();
		RequestBody body = RequestBody.fromString("part");
		UploadPartResponse expected = UploadPartResponse.builder().eTag("etag").build();

		when(mockBucketClient.uploadPart(request, body)).thenReturn(expected);

		// call under test
		UploadPartResponse result = client.uploadPartV2(request, body);

		assertEquals(expected, result);
		verify(mockBucketClient).uploadPart(request, body);
	}

	@Test
	public void testUploadPartCopyV2() {
		setupBucketClient();

		UploadPartCopyRequest request = UploadPartCopyRequest.builder().sourceBucket("source-bucket")
				.sourceKey("source-key").destinationBucket(BUCKET_NAME).destinationKey(OBJECT_KEY)
				.uploadId(UPLOAD_ID).partNumber(1).build();
		UploadPartCopyResponse expected = UploadPartCopyResponse.builder().build();

		when(mockBucketClient.uploadPartCopy(request)).thenReturn(expected);

		// call under test
		UploadPartCopyResponse result = client.uploadPartCopyV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).uploadPartCopy(request);
	}

	@Test
	public void testCompleteMultipartUploadV2() {
		setupBucketClient();

		CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder().bucket(BUCKET_NAME)
				.key(OBJECT_KEY).uploadId(UPLOAD_ID).build();
		CompleteMultipartUploadResponse expected = CompleteMultipartUploadResponse.builder().eTag("etag").build();

		when(mockBucketClient.completeMultipartUpload(request)).thenReturn(expected);

		// call under test
		CompleteMultipartUploadResponse result = client.completeMultipartUploadV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).completeMultipartUpload(request);
	}

	@Test
	public void testAbortMultipartUploadV2() {
		setupBucketClient();

		AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder().bucket(BUCKET_NAME)
				.key(OBJECT_KEY).uploadId(UPLOAD_ID).build();
		AbortMultipartUploadResponse expected = AbortMultipartUploadResponse.builder().build();

		when(mockBucketClient.abortMultipartUpload(request)).thenReturn(expected);

		// call under test
		AbortMultipartUploadResponse result = client.abortMultipartUploadV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).abortMultipartUpload(request);
	}

	@Test
	public void testGetObjectTagsV2() {
		setupBucketClient();

		List<Tag> tags = Arrays.asList(Tag.builder().key("key").value("value").build(),
				Tag.builder().key("key2").value("value").build());

		when(mockBucketClient.getObjectTagging(any(GetObjectTaggingRequest.class)))
				.thenReturn(GetObjectTaggingResponse.builder().tagSet(tags).build());

		// call under test
		List<Tag> result = client.getObjectTagsV2(BUCKET_NAME, OBJECT_KEY);

		assertEquals(tags, result);
		verify(mockBucketClient)
				.getObjectTagging(GetObjectTaggingRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build());
	}

	@Test
	public void testSetObjectTagsV2() {
		setupBucketClient();

		List<Tag> tags = Arrays.asList(Tag.builder().key("key").value("value").build(),
				Tag.builder().key("key2").value("value").build());

		when(mockBucketClient.putObjectTagging(any(PutObjectTaggingRequest.class)))
				.thenReturn(PutObjectTaggingResponse.builder().build());

		// call under test
		client.setObjectTagsV2(BUCKET_NAME, OBJECT_KEY, tags);

		verify(mockBucketClient).putObjectTagging(PutObjectTaggingRequest.builder().bucket(BUCKET_NAME)
				.key(OBJECT_KEY).tagging(Tagging.builder().tagSet(tags).build()).build());
	}

	@Test
	public void testRestoreObjectV2() {
		setupBucketClient();

		RestoreObjectRequest request = RestoreObjectRequest.builder().bucket(BUCKET_NAME).key(OBJECT_KEY).build();
		RestoreObjectResponse expected = RestoreObjectResponse.builder().build();

		when(mockBucketClient.restoreObject(request)).thenReturn(expected);

		// call under test
		RestoreObjectResponse result = client.restoreObjectV2(request);

		assertEquals(expected, result);
		verify(mockBucketClient).restoreObject(request);
	}

}
