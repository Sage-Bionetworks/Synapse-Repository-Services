package org.sagebionetworks.aws.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
public class S3ObjectStoreImplUnitTest {

	@Mock
	private S3Client mockS3Client;

	@Mock
	private S3Presigner mockPresigner;

	@Mock
	private PresignedGetObjectRequest mockPresignedRequest;

	private Map<Region, Integer> presignerBuildCount;

	private S3ObjectStoreImpl objectStore;

	private File tempFile;

	@BeforeEach
	public void before() {
		presignerBuildCount = new HashMap<Region, Integer>();
		objectStore = new S3ObjectStoreImpl(mockS3Client, region -> {
			presignerBuildCount.merge(region, 1, Integer::sum);
			return mockPresigner;
		});
	}

	@AfterEach
	public void after() {
		if (tempFile != null) {
			tempFile.delete();
		}
	}

	@Test
	public void testGetObject() throws Exception {
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream("the content"));

		// call under test
		InputStream result = objectStore.getObject("some.bucket", "some/key");

		assertEquals("the content", readAll(result));
		verify(mockS3Client).getObject(GetObjectRequest.builder().bucket("some.bucket").key("some/key").build());
	}

	@Test
	public void testGetObjectWithInaccessibleBucket() {
		when(mockS3Client.getObject(any(GetObjectRequest.class)))
				.thenThrow(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchBucket"));

		// call under test
		CannotDetermineBucketLocationException e = assertThrows(CannotDetermineBucketLocationException.class,
				() -> objectStore.getObject("some.bucket", "some/key"));

		assertTrue(e.getMessage().contains("some.bucket"));
	}

	@Test
	public void testGetObjectWithForbidden() {
		when(mockS3Client.getObject(any(GetObjectRequest.class)))
				.thenThrow(s3Exception(HttpStatusCode.FORBIDDEN, "AccessDenied"));

		// call under test
		assertThrows(CannotDetermineBucketLocationException.class,
				() -> objectStore.getObject("some.bucket", "some/key"));
	}

	@Test
	public void testGetObjectWithMissingKey() {
		S3Exception notFound = s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchKey");
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenThrow(notFound);

		// A failure scoped to the object is not a bucket failure, so it passes through untranslated.
		S3Exception result = assertThrows(S3Exception.class, () -> objectStore.getObject("some.bucket", "some/key"));

		assertSame(notFound, result);
	}

	@Test
	public void testGetObjectWithNullBucket() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> objectStore.getObject(null, "some/key"));

		assertEquals("bucket is required.", e.getMessage());
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetObjectWithEmptyKey() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> objectStore.getObject("some.bucket", ""));

		assertEquals("key is required.", e.getMessage());
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetObjectWithFile() throws Exception {
		// Callers hand over a temp file that already exists, so the write must overwrite it.
		tempFile = File.createTempFile("S3ObjectStoreImplUnitTest", ".txt");
		Files.write(tempFile.toPath(), "stale content".getBytes(StandardCharsets.UTF_8));

		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream("fresh content"));

		// call under test
		objectStore.getObject("some.bucket", "some/key", tempFile);

		assertEquals("fresh content", new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8));
	}

	@Test
	public void testGetObjectWithNullFile() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> objectStore.getObject("some.bucket", "some/key", null));

		assertEquals("destination is required.", e.getMessage());
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testGetObjectInfo() {
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
				.contentType("text/plain")
				.contentEncoding("gzip")
				.eTag("some-etag")
				.contentLength(123L)
				.archiveStatus("DEEP_ARCHIVE_ACCESS")
				.restore("ongoing-request=\"true\"")
				.build());

		// call under test
		Optional<S3ObjectInfo> result = objectStore.getObjectInfo("some.bucket", "some/key");

		S3ObjectInfo expected = S3ObjectInfo.builder()
				.withContentType("text/plain")
				.withContentEncoding("gzip")
				.withEtag("some-etag")
				.withContentLength(123L)
				.withArchiveStatus("DEEP_ARCHIVE_ACCESS")
				.withOngoingRestore(Boolean.TRUE)
				.build();

		assertEquals(Optional.of(expected), result);
		verify(mockS3Client).headObject(HeadObjectRequest.builder().bucket("some.bucket").key("some/key").build());
	}

	@Test
	public void testGetObjectInfoWithMissingKey() {
		when(mockS3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(s3Exception(HttpStatusCode.NOT_FOUND, null));

		// call under test
		Optional<S3ObjectInfo> result = objectStore.getObjectInfo("some.bucket", "some/key");

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetObjectInfoWithInaccessibleBucket() {
		// A missing bucket is also reported as a 404, so the bucket check must win over the empty result.
		when(mockS3Client.headObject(any(HeadObjectRequest.class)))
				.thenThrow(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchBucket"));

		// call under test
		assertThrows(CannotDetermineBucketLocationException.class,
				() -> objectStore.getObjectInfo("some.bucket", "some/key"));
	}

	@Test
	public void testGetObjectInfoWithServerError() {
		S3Exception serverError = s3Exception(500, "InternalError");
		when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(serverError);

		// call under test
		S3Exception result = assertThrows(S3Exception.class, () -> objectStore.getObjectInfo("some.bucket", "some/key"));

		assertSame(serverError, result);
	}

	@Test
	public void testParseOngoingRestoreWithNullHeader() {
		// call under test
		assertNull(S3ObjectInfo.parseOngoingRestore(null));
	}

	@Test
	public void testParseOngoingRestoreWithOngoingRequest() {
		// call under test
		assertEquals(Boolean.TRUE, S3ObjectInfo.parseOngoingRestore("ongoing-request=\"true\""));
	}

	@Test
	public void testParseOngoingRestoreWithCompletedRequest() {
		// call under test
		assertEquals(Boolean.FALSE, S3ObjectInfo
				.parseOngoingRestore("ongoing-request=\"false\", expiry-date=\"Fri, 21 Dec 2012 00:00:00 GMT\""));
	}

	@Test
	public void testParseOngoingRestoreWithUnexpectedHeader() {
		// call under test
		assertNull(S3ObjectInfo.parseOngoingRestore("expiry-date=\"Fri, 21 Dec 2012 00:00:00 GMT\""));
	}

	@Test
	public void testPutObjectWithInputStream() {
		S3WriteOptions options = S3WriteOptions.builder()
				.withContentType("text/plain")
				.withContentEncoding("gzip")
				.withContentDisposition("attachment; filename=foo.txt")
				.withContentMd5Base64("rL0Y20zC+Fzt72VPzMSk2A==")
				.withStorageClass(S3StorageClass.INTELLIGENT_TIERING)
				.withCannedAcl(S3CannedAcl.BUCKET_OWNER_FULL_CONTROL)
				.build();

		byte[] content = "the content".getBytes(StandardCharsets.UTF_8);

		// call under test
		objectStore.putObject("some.bucket", "some/key", new ByteArrayInputStream(content), content.length, options);

		ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
		verify(mockS3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

		PutObjectRequest expected = PutObjectRequest.builder()
				.bucket("some.bucket")
				.key("some/key")
				.contentType("text/plain")
				.contentEncoding("gzip")
				.contentDisposition("attachment; filename=foo.txt")
				.contentMD5("rL0Y20zC+Fzt72VPzMSk2A==")
				.storageClass(StorageClass.INTELLIGENT_TIERING)
				.acl(ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
				.build();

		assertEquals(expected, requestCaptor.getValue());
		assertEquals(content.length, bodyCaptor.getValue().contentLength());
	}

	@Test
	public void testPutObjectWithEmptyOptions() {
		byte[] content = "the content".getBytes(StandardCharsets.UTF_8);

		// call under test
		objectStore.putObject("some.bucket", "some/key", new ByteArrayInputStream(content), content.length,
				S3WriteOptions.empty());

		ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(mockS3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

		assertEquals(PutObjectRequest.builder().bucket("some.bucket").key("some/key").build(),
				requestCaptor.getValue());
	}

	@Test
	public void testPutObjectWithNullOptions() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> objectStore
				.putObject("some.bucket", "some/key", new ByteArrayInputStream(new byte[0]), 0L, null));

		assertEquals("options is required, use S3WriteOptions.empty() for S3 defaults.", e.getMessage());
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testPutObjectWithFile() throws Exception {
		tempFile = File.createTempFile("S3ObjectStoreImplUnitTest", ".txt");
		Files.write(tempFile.toPath(), "the content".getBytes(StandardCharsets.UTF_8));

		S3WriteOptions options = S3WriteOptions.builder().withStorageClass(S3StorageClass.STANDARD).build();

		// call under test
		objectStore.putObject("some.bucket", "some/key", tempFile, options);

		ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(mockS3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

		assertEquals(PutObjectRequest.builder()
				.bucket("some.bucket")
				.key("some/key")
				.storageClass(StorageClass.STANDARD)
				.build(), requestCaptor.getValue());
	}

	@Test
	public void testDeleteObject() {
		// call under test
		objectStore.deleteObject("some.bucket", "some/key");

		verify(mockS3Client).deleteObject(DeleteObjectRequest.builder().bucket("some.bucket").key("some/key").build());
	}

	@Test
	public void testGetObjectTags() {
		when(mockS3Client.getObjectTagging(any(GetObjectTaggingRequest.class)))
				.thenReturn(GetObjectTaggingResponse.builder()
						.tagSet(Arrays.asList(Tag.builder().key("one").value("1").build(),
								Tag.builder().key("two").value("2").build()))
						.build());

		// call under test
		List<S3ObjectTag> result = objectStore.getObjectTags("some.bucket", "some/key");

		assertEquals(Arrays.asList(new S3ObjectTag("one", "1"), new S3ObjectTag("two", "2")), result);
		verify(mockS3Client)
				.getObjectTagging(GetObjectTaggingRequest.builder().bucket("some.bucket").key("some/key").build());
	}

	@Test
	public void testGetObjectTagsWithInaccessibleBucket() {
		when(mockS3Client.getObjectTagging(any(GetObjectTaggingRequest.class)))
				.thenThrow(s3Exception(HttpStatusCode.MOVED_PERMANENTLY, "PermanentRedirect"));

		// call under test
		assertThrows(CannotDetermineBucketLocationException.class,
				() -> objectStore.getObjectTags("some.bucket", "some/key"));
	}

	@Test
	public void testSetObjectTags() {
		// call under test
		objectStore.setObjectTags("some.bucket", "some/key", Collections.singletonList(new S3ObjectTag("one", "1")));

		ArgumentCaptor<PutObjectTaggingRequest> requestCaptor = ArgumentCaptor
				.forClass(PutObjectTaggingRequest.class);
		verify(mockS3Client).putObjectTagging(requestCaptor.capture());

		PutObjectTaggingRequest request = requestCaptor.getValue();
		assertEquals("some.bucket", request.bucket());
		assertEquals("some/key", request.key());
		assertEquals(Collections.singletonList(Tag.builder().key("one").value("1").build()),
				request.tagging().tagSet());
	}

	@Test
	public void testSetObjectTagsWithNullTags() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> objectStore.setObjectTags("some.bucket", "some/key", null));

		assertEquals("tags is required.", e.getMessage());
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testPresignGetObject() throws Exception {
		URL url = new URL("https://some.bucket.s3.us-west-2.amazonaws.com/some/key?signature");
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-west-2").build());
		when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
		when(mockPresignedRequest.url()).thenReturn(url);

		// call under test
		URL result = objectStore.presignGetObject("some.bucket", "some/key", Duration.ofMinutes(30));

		assertEquals(url, result);

		// The pre-signer must be built for the region the bucket actually lives in.
		assertEquals(Collections.singletonMap(Region.US_WEST_2, 1), presignerBuildCount);

		ArgumentCaptor<GetObjectPresignRequest> requestCaptor = ArgumentCaptor
				.forClass(GetObjectPresignRequest.class);
		verify(mockPresigner).presignGetObject(requestCaptor.capture());

		GetObjectPresignRequest request = requestCaptor.getValue();
		assertEquals(Duration.ofMinutes(30), request.signatureDuration());
		assertEquals(GetObjectRequest.builder().bucket("some.bucket").key("some/key").build(),
				request.getObjectRequest());
	}

	@Test
	public void testPresignGetObjectWithResponseHeaders() throws Exception {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-east-1").build());
		when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
		when(mockPresignedRequest.url()).thenReturn(new URL("https://example.com/some/key?signature"));

		S3ResponseHeaders responseHeaders = new S3ResponseHeaders("text/plain", "attachment; filename=foo.txt");

		// call under test
		objectStore.presignGetObject("some.bucket", "some/key", Duration.ofMinutes(30), responseHeaders);

		ArgumentCaptor<GetObjectPresignRequest> requestCaptor = ArgumentCaptor
				.forClass(GetObjectPresignRequest.class);
		verify(mockPresigner).presignGetObject(requestCaptor.capture());

		assertEquals(GetObjectRequest.builder()
				.bucket("some.bucket")
				.key("some/key")
				.responseContentType("text/plain")
				.responseContentDisposition("attachment; filename=foo.txt")
				.build(), requestCaptor.getValue().getObjectRequest());
	}

	@Test
	public void testPresignGetObjectWithNullExpiration() {
		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> objectStore.presignGetObject("some.bucket", "some/key", null));

		assertEquals("expiration is required.", e.getMessage());
		verifyNoInteractions(mockS3Client);
	}

	@Test
	public void testPresignGetObjectWithRepeatedCalls() throws Exception {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-west-2").build());
		when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
		when(mockPresignedRequest.url()).thenReturn(new URL("https://example.com/some/key?signature"));

		// call under test
		objectStore.presignGetObject("some.bucket", "some/key", Duration.ofMinutes(30));
		objectStore.presignGetObject("some.bucket", "other/key", Duration.ofMinutes(30));

		// Both the resolved region and the pre-signer built from it are cached.
		verify(mockS3Client, times(1)).headBucket(any(HeadBucketRequest.class));
		assertEquals(Collections.singletonMap(Region.US_WEST_2, 1), presignerBuildCount);
	}

	@Test
	public void testPresignGetObjectWithBucketsInDifferentRegions() throws Exception {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-west-2").build(),
						HeadBucketResponse.builder().bucketRegion("eu-central-1").build());
		when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
		when(mockPresignedRequest.url()).thenReturn(new URL("https://example.com/some/key?signature"));

		// call under test
		objectStore.presignGetObject("west.bucket", "some/key", Duration.ofMinutes(30));
		objectStore.presignGetObject("central.bucket", "some/key", Duration.ofMinutes(30));

		Map<Region, Integer> expected = new HashMap<Region, Integer>();
		expected.put(Region.US_WEST_2, 1);
		expected.put(Region.EU_CENTRAL_1, 1);

		assertEquals(expected, presignerBuildCount);
	}

	@Test
	public void testPresignGetObjectWithoutReportedRegion() throws Exception {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
		when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
		when(mockPresignedRequest.url()).thenReturn(new URL("https://example.com/some/key?signature"));

		// call under test
		objectStore.presignGetObject("some.bucket", "some/key", Duration.ofMinutes(30));

		assertEquals(Collections.singletonMap(Region.US_EAST_1, 1), presignerBuildCount);
	}

	@Test
	public void testVerifyBucketAccess() {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-east-1").build());

		// call under test
		objectStore.verifyBucketAccess("some.bucket");

		verify(mockS3Client).headBucket(HeadBucketRequest.builder().bucket("some.bucket").build());
		verify(mockPresigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
	}

	@Test
	public void testVerifyBucketAccessWithInaccessibleBucket() {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(s3Exception(HttpStatusCode.NOT_FOUND, "NoSuchBucket"));

		// call under test
		CannotDetermineBucketLocationException e = assertThrows(CannotDetermineBucketLocationException.class,
				() -> objectStore.verifyBucketAccess("some.bucket"));

		assertTrue(e.getMessage().contains("some.bucket"));
	}

	@Test
	public void testVerifyBucketAccessWithServerError() {
		// Any failure to head a bucket means its location is unknown, whatever the reported cause.
		when(mockS3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Exception(500, "InternalError"));

		// call under test
		assertThrows(CannotDetermineBucketLocationException.class, () -> objectStore.verifyBucketAccess("some.bucket"));
	}

	@Test
	public void testIsSameRegionWithMatchingRegions() {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-west-2").build());

		// call under test
		assertTrue(objectStore.isSameRegion("one.bucket", "two.bucket"));
	}

	@Test
	public void testIsSameRegionWithDifferentRegions() {
		when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("us-west-2").build(),
						HeadBucketResponse.builder().bucketRegion("eu-central-1").build());

		// call under test
		assertFalse(objectStore.isSameRegion("one.bucket", "two.bucket"));
	}

	private static ResponseInputStream<GetObjectResponse> responseStream(String content) {
		return new ResponseInputStream<GetObjectResponse>(GetObjectResponse.builder().build(),
				new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
	}

	private static String readAll(InputStream in) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	private static S3Exception s3Exception(int statusCode, String errorCode) {
		return (S3Exception) S3Exception.builder()
				.statusCode(statusCode)
				.awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
				.build();
	}

}
