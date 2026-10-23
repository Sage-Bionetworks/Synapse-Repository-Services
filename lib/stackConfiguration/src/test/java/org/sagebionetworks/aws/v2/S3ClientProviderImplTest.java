package org.sagebionetworks.aws.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
public class S3ClientProviderImplTest {

	@Mock
	private Function<Region, S3Client> mockClientFactory;

	@Mock
	private S3Client mockUsEast1Client;

	@Mock
	private S3Client mockUsWest1Client;

	private S3ClientProviderImpl provider;

	private static final String BUCKET_NAME = "bucket-name";

	private static final Region BUCKET_REGION = Region.US_WEST_1;

	@BeforeEach
	public void before() {
		provider = new S3ClientProviderImpl(mockClientFactory);
	}

	@Test
	public void testGetRegionForBucket() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion(BUCKET_REGION.id()).build());

		// call under test
		Region result = provider.getRegionForBucket(BUCKET_NAME);

		assertEquals(BUCKET_REGION, result);

		ArgumentCaptor<HeadBucketRequest> requestCaptor = ArgumentCaptor.forClass(HeadBucketRequest.class);

		verify(mockUsEast1Client).headBucket(requestCaptor.capture());

		assertEquals(HeadBucketRequest.builder().bucket(BUCKET_NAME).build(), requestCaptor.getValue());
	}

	@Test
	public void testGetRegionForBucketWithNullBucketRegion() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion(null).build());

		// call under test
		Region result = provider.getRegionForBucket(BUCKET_NAME);

		assertEquals(Region.US_EAST_1, result);
	}

	@Test
	public void testGetRegionForBucketWithEmptyBucketRegion() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion("").build());

		// call under test
		Region result = provider.getRegionForBucket(BUCKET_NAME);

		assertEquals(Region.US_EAST_1, result);
	}

	@Test
	public void testGetRegionForBucketWithCachedRegion() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion(BUCKET_REGION.id()).build());

		// call under test
		assertEquals(BUCKET_REGION, provider.getRegionForBucket(BUCKET_NAME));
		assertEquals(BUCKET_REGION, provider.getRegionForBucket(BUCKET_NAME));

		verify(mockUsEast1Client, times(1)).headBucket(any(HeadBucketRequest.class));
	}

	@Test
	public void testGetRegionForBucketWithNullBucketName() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getRegionForBucket(null);
		}).getMessage();

		assertEquals("bucketName is required.", message);

		verifyNoInteractions(mockClientFactory);
	}

	@Test
	public void testGetRegionForBucketWithEmptyBucketName() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getRegionForBucket(" ");
		}).getMessage();

		assertEquals("bucketName is required.", message);

		verifyNoInteractions(mockClientFactory);
	}

	@Test
	public void testGetRegionForBucketWithMissingBucket() {
		NoSuchBucketException s3Exception = (NoSuchBucketException) NoSuchBucketException.builder().statusCode(404)
				.message("no such bucket").build();

		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Exception);

		CannotDetermineBucketLocationException result = assertThrows(CannotDetermineBucketLocationException.class,
				() -> {
					// call under test
					provider.getRegionForBucket(BUCKET_NAME);
				});

		assertEquals("Failed to determine the Amazon region for bucket '" + BUCKET_NAME
				+ "'. Please ensure that the bucket exists, is shared with Synapse, in particular granting ListObject permission.",
				result.getMessage());
		assertEquals(s3Exception, result.getCause());
	}

	@Test
	public void testGetRegionForBucketWithPermanentRedirect() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(redirectException(301, BUCKET_REGION.id()));

		// call under test
		Region result = provider.getRegionForBucket(BUCKET_NAME);

		assertEquals(BUCKET_REGION, result);
	}

	@Test
	public void testGetRegionForBucketWithPermanentRedirectAndNoRegionHeader() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class))).thenThrow(redirectException(301, null));

		assertThrows(CannotDetermineBucketLocationException.class, () -> {
			// call under test
			provider.getRegionForBucket(BUCKET_NAME);
		});
	}

	@Test
	public void testGetRegionForBucketWithForbiddenAndRegionHeader() {
		// A bucket that does not grant access cannot be used even when its region is disclosed
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(redirectException(403, BUCKET_REGION.id()));

		assertThrows(CannotDetermineBucketLocationException.class, () -> {
			// call under test
			provider.getRegionForBucket(BUCKET_NAME);
		});
	}

	@Test
	public void testGetClientForBucket() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockClientFactory.apply(BUCKET_REGION)).thenReturn(mockUsWest1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion(BUCKET_REGION.id()).build());

		// call under test
		S3Client result = provider.getClientForBucket(BUCKET_NAME);

		assertSame(mockUsWest1Client, result);
	}

	@Test
	public void testGetClientForBucketWithUsEast1Bucket() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenReturn(HeadBucketResponse.builder().bucketRegion(Region.US_EAST_1.id()).build());

		// call under test
		S3Client result = provider.getClientForBucket(BUCKET_NAME);

		assertSame(mockUsEast1Client, result);
	}

	@Test
	public void testGetClientForBucketWithMissingBucket() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);
		when(mockUsEast1Client.headBucket(any(HeadBucketRequest.class)))
				.thenThrow((NoSuchBucketException) NoSuchBucketException.builder().statusCode(404).build());

		assertThrows(CannotDetermineBucketLocationException.class, () -> {
			// call under test
			provider.getClientForBucket(BUCKET_NAME);
		});

		verify(mockClientFactory, never()).apply(BUCKET_REGION);
	}

	@Test
	public void testGetUsEast1Client() {
		when(mockClientFactory.apply(Region.US_EAST_1)).thenReturn(mockUsEast1Client);

		// call under test
		assertSame(mockUsEast1Client, provider.getUsEast1Client());
		assertSame(mockUsEast1Client, provider.getUsEast1Client());

		// the client of a region is built at most once
		verify(mockClientFactory, times(1)).apply(Region.US_EAST_1);
	}

	private static S3Exception redirectException(int statusCode, String bucketRegion) {
		SdkHttpResponse.Builder httpResponseBuilder = SdkHttpResponse.builder().statusCode(statusCode);

		if (bucketRegion != null) {
			httpResponseBuilder.appendHeader("x-amz-bucket-region", bucketRegion);
		}

		return (S3Exception) S3Exception.builder()
				.statusCode(statusCode)
				.awsErrorDetails(AwsErrorDetails.builder().sdkHttpResponse(httpResponseBuilder.build()).build())
				.build();
	}

}
