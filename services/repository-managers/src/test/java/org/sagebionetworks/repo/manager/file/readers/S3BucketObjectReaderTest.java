package org.sagebionetworks.repo.manager.file.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.util.AmazonErrorCodes;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
public class S3BucketObjectReaderTest {

	private static final String BUCKET_NAME = "somebucket";
	private static final String OBJECT_KEY = "someObjectKey";

	private static final GetObjectRequest EXPECTED_REQUEST = GetObjectRequest.builder().bucket(BUCKET_NAME)
			.key(OBJECT_KEY).build();

	@Mock
	private SynapseS3Client mockS3Client;

	@InjectMocks
	private S3BucketObjectReader objectReader;

	@Mock
	private ResponseInputStream<GetObjectResponse> mockObjectStream;

	private static S3Exception s3Exception(int statusCode, String errorCode, String message) {
		return (S3Exception) S3Exception.builder().statusCode(statusCode).message(message)
				.awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).errorMessage(message).build())
				.build();
	}

	@Test
	public void testVerifyBucketAccess() {
		when(mockS3Client.getRegionForBucketV2(BUCKET_NAME)).thenReturn(Region.US_EAST_2);

		// Call under test
		objectReader.verifyBucketAccess(BUCKET_NAME);

		verify(mockS3Client).getRegionForBucketV2(BUCKET_NAME);
	}

	@Test
	public void testOpenStream() {
		when(mockS3Client.getObjectV2(EXPECTED_REQUEST)).thenReturn(mockObjectStream);

		// Call under test
		InputStream stream = objectReader.openStream(BUCKET_NAME, OBJECT_KEY);

		assertSame(mockObjectStream, stream);
		verify(mockS3Client).getObjectV2(EXPECTED_REQUEST);
	}

	@Test
	public void testOpenStreamWithS3_BUCKET_NOT_FOUND_Exception() {
		when(mockS3Client.getObjectV2(EXPECTED_REQUEST))
				.thenThrow(s3Exception(404, AmazonErrorCodes.S3_BUCKET_NOT_FOUND, "no bucket"));

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find S3 bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testOpenStreamWithS3_KEY_NOT_FOUND_Exception() {
		when(mockS3Client.getObjectV2(EXPECTED_REQUEST))
				.thenThrow(s3Exception(404, AmazonErrorCodes.S3_KEY_NOT_FOUND, "no key"));

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testOpenStreamWithNotFoundStatusAndNoErrorCode() {
		when(mockS3Client.getObjectV2(EXPECTED_REQUEST))
				.thenThrow((S3Exception) S3Exception.builder().statusCode(404).message("Not Found").build());

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testOpenStreamWithOtherAwsException() {
		S3Exception awsException = s3Exception(500, "InternalError", "Some AWS error");

		when(mockS3Client.getObjectV2(EXPECTED_REQUEST)).thenThrow(awsException);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not read S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME + ": "
				+ awsException.getMessage(), e.getMessage());
		assertSame(awsException, e.getCause());
	}

	@Test
	public void testOpenStreamWithOtherException() {
		RuntimeException cause = new RuntimeException("Some other error");

		when(mockS3Client.getObjectV2(EXPECTED_REQUEST)).thenThrow(cause);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not read S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME
				+ ": Some other error", e.getMessage());
		assertSame(cause, e.getCause());
	}

}
