package org.sagebionetworks.repo.manager.storagelocation.objectreaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.util.AmazonErrorCodes;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

@ExtendWith(MockitoExtension.class)
public class S3BucketObjectReaderTest {

	private static final String BUCKET_NAME = "somebucket";
	private static final String OBJECT_KEY = "someObjectKey";

	@Mock
	private SynapseS3Client mockS3Client;

	@InjectMocks
	private S3BucketObjectReader objectReader;

	@Mock
	private S3Object mockS3Object;

	@Mock
	private S3ObjectInputStream mockInputStream;

	@Mock
	private AmazonServiceException mockAWSException;

	@Test
	public void testVerifyBucketAccess() {
		when(mockS3Client.getRegionForBucket(BUCKET_NAME)).thenReturn(Region.US_East_2);

		// Call under test
		objectReader.verifyBucketAccess(BUCKET_NAME);

		verify(mockS3Client).getRegionForBucket(BUCKET_NAME);
	}

	@Test
	public void testGetSupportedStorageLocationType() {
		assertEquals(ExternalS3StorageLocationSetting.class, objectReader.getSupportedStorageLocationType());
	}

	@Test
	public void testOpenStream() {
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockInputStream);

		// Call under test
		InputStream stream = objectReader.openStream(BUCKET_NAME, OBJECT_KEY);

		assertEquals(mockInputStream, stream);
		verify(mockS3Client).getObject(BUCKET_NAME, OBJECT_KEY);
		verify(mockS3Object).getObjectContent();
	}

	@Test
	public void testOpenStreamWithS3_BUCKET_NOT_FOUND_Exception() {
		when(mockAWSException.getErrorCode()).thenReturn(AmazonErrorCodes.S3_BUCKET_NOT_FOUND);
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(mockAWSException);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find S3 bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testOpenStreamWithS3_KEY_NOT_FOUND_Exception() {
		when(mockAWSException.getErrorCode()).thenReturn(AmazonErrorCodes.S3_KEY_NOT_FOUND);
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(mockAWSException);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testOpenStreamWithS3_NOT_FOUND_Exception() {
		when(mockAWSException.getErrorCode()).thenReturn(AmazonErrorCodes.S3_NOT_FOUND);
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(mockAWSException);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Did not find S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME, e.getMessage());
	}

	@Test
	public void testOpenStreamWithOtherException() throws IOException {
		String errorMsg = "Some AWS error";
		when(mockAWSException.getMessage()).thenReturn(errorMsg);
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(mockAWSException);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		assertEquals("Could not read S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME + ": " + errorMsg, e.getMessage());
	}
	
	@Test
	public void testOpenStreamWithExceptionOnRead() throws IOException {
		String errorMsg = "Some AWS error";
		
		when(mockAWSException.getMessage()).thenReturn(errorMsg);
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenThrow(mockAWSException);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		verify(mockS3Object).close();
		assertEquals("Could not read S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME + ": " + errorMsg, e.getMessage());
	}
	
	@Test
	public void testOpenStreamWithExceptionOnClose() throws IOException {
		String errorMsg = "Some AWS error";
		
		when(mockAWSException.getMessage()).thenReturn(errorMsg);
		when(mockS3Client.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenThrow(mockAWSException);
		
		IOException ex = new IOException("Some I/O exception");
		
		doThrow(ex).when(mockS3Object).close();

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			objectReader.openStream(BUCKET_NAME, OBJECT_KEY);
		});

		verify(mockS3Object).close();
		assertEquals("Could not read S3 object at key " + OBJECT_KEY + " from bucket " + BUCKET_NAME + ": " + errorMsg, e.getMessage());
	}

}
